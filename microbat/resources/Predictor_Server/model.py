# Copyright (c) Microsoft Corporation. 
# Licensed under the MIT license.

import torch
import torch.nn as nn
import torch.nn.functional as F

classifier_id1 = {'<ROOT>':0,'<CORRECT>':1,'<CONTROL>':2,'<DATA>':3}
classifier_id2 = {'<TRUE>':0,'<FALSE>':1}

class FocalLoss(nn.Module):
    def __init__(self, alpha=1, gamma=5):
        super(FocalLoss, self).__init__()
        self.alpha = alpha
        self.gamma = gamma
        # self.num_classes = num_classes

    def forward(self, inputs, targets):
        ce_loss = F.cross_entropy(inputs, targets, reduction='none') # -log(pt)
        pt = torch.exp(-ce_loss)
        F_loss = self.alpha * (1-pt)**self.gamma * ce_loss
        return F_loss.mean()


class mlmodel(nn.Module):
    def __init__(self, encoder,config,tokenizer,device,use_focal_loss=True,beam_size=None,max_length=None,sos_id=None,eos_id=None,mask1_id=None,mask2_id=None):
        super(mlmodel, self).__init__()
        self.encoder = encoder
        self.config=config
        self.tokenizer = tokenizer
        self.device = device
        self.use_focal_loss = use_focal_loss
        # self.register_buffer("bias", torch.tril(torch.ones(2048, 2048)))

        self.dense = nn.Linear(config.hidden_size, config.hidden_size)
        self.lm_head_1 = nn.Linear(config.hidden_size, 4, bias=False)
        self.lm_head_2 = nn.Linear(config.hidden_size, 2 ,bias=False)
        self.lsm = nn.LogSoftmax(dim=-1)

        self.max_length=max_length
        self.mask1_id=mask1_id
        self.mask2_id=mask2_id

        # self.tie_weights()
        # self.beam_size=beam_size
        # self.sos_id=sos_id
        # self.eos_id=eos_id
        # self.label_weight = torch.zeros(config.vocab_size)
        # self.label_weight[self.mask_id] = 1.0

        # vocab_size: 32100 + len(special_tokens) = len(tokenizer)
        # hidden_size: 768
        
    def _tie_or_clone_weights(self, first_module, second_module):
        """ 
        Tie or clone module weights depending of weither we are using TorchScript or not
        """
        if self.config.torchscript:
            first_module.weight = nn.Parameter(second_module.weight.clone())
        else:
            first_module.weight = second_module.weight
                  
    def tie_weights(self):
        """ Make sure we are sharing the input and output embeddings.
            Export to TorchScript can't handle parameter sharing so we are cloning them instead.
        """
        self._tie_or_clone_weights(self.lm_head_1, self.encoder.embed_tokens)
        # self._tie_or_clone_weights(self.lm_head, self.encoder.embeddings.word_embeddings)  
                                   
    def forward(self, source_ids=None,source_mask=None,target_ids=None,type = "infer"):
        outputs = self.encoder(source_ids, attention_mask=source_mask)
        encoder_output = outputs[0].permute([1,0,2]).contiguous() # (max_len,batch_size,hidden_size)
        hidden_states = torch.tanh(self.dense(encoder_output)).permute([1,0,2]).contiguous() # (batch_size,max_len,hidden_size)
        
        lm_logits_1 = self.lm_head_1(hidden_states).contiguous() # (batch_size,max_len, 4)
        lm_logits_2 = self.lm_head_2(hidden_states).contiguous() # (batch_size,max_len, 2)
        
        if type == "train" or type == "eval":
            # filter logist that related to MASK
            active_loss_1 = (source_ids == self.mask1_id).contiguous().view(-1) # find which tokens are masked 1
            labels_ids1 = target_ids.contiguous().view(-1)[active_loss_1] # get the labels of the masked tokens
            labels_1 = torch.tensor([classifier_id1[self.tokenizer.decode(x,clean_up_tokenization_spaces=False)] for x in labels_ids1]).to(self.device)
            filtered_logits_1 = lm_logits_1.contiguous().view(-1, 4)[active_loss_1] # get the logits of the masked tokens

            # only consider the loss of mask2 when mask1 is <DATA>
            data_mask = (labels_1 == 3).contiguous().view(-1)
            filtered_source_ids = source_ids[data_mask]
            filtered_target_ids = target_ids[data_mask]
            filtered_lm_logits_2 = lm_logits_2[data_mask]

            if len(filtered_source_ids) > 0:
                active_loss_2 = (filtered_source_ids == self.mask2_id).contiguous().view(-1) # find which tokens are masked 2
                labels_ids2 = filtered_target_ids.contiguous().view(-1)[active_loss_2] # get the labels of the masked tokens
                labels_2 = torch.tensor([classifier_id2[self.tokenizer.decode(x,clean_up_tokenization_spaces=False)] for x in labels_ids2]).to(self.device)
                filtered_logits_2 = filtered_lm_logits_2.contiguous().view(-1, 2)[active_loss_2] # get the logits of the masked tokens
            else:
                filtered_logits_2 = torch.empty(0, 2, dtype=lm_logits_2.dtype, device=lm_logits_2.device)

            if type == "train":
                if self.use_focal_loss:
                    loss_func = FocalLoss()
                else:
                    loss_func = nn.CrossEntropyLoss(ignore_index=-1)

                # loss for mask1
                loss1 = loss_func(filtered_logits_1, labels_1)
                # loss for mask2
                if len(filtered_source_ids)>0:
                    loss2 = loss_func(filtered_logits_2, labels_2)
                else:
                    loss2 = 0
                return loss1,loss2
            
            else: # eval
                return filtered_logits_1,filtered_logits_2
            
        else: # infer, target_ids = None
            active_loss_1 = (source_ids == self.mask1_id).contiguous().view(-1) # find which tokens are masked 1
            filtered_logits_1 = lm_logits_1.contiguous().view(-1, 4)[active_loss_1] # get the logits of the masked tokens
            active_loss_2 = (source_ids == self.mask2_id).contiguous().view(-1) # find which tokens are masked 2
            filtered_logits_2 = lm_logits_2.contiguous().view(-1, 2)[active_loss_2] # get the logits of the masked tokens

            filtered_logits_1 = F.softmax(filtered_logits_1, dim=-1)
            filtered_logits_2 = F.softmax(filtered_logits_2, dim=-1)
            return filtered_logits_1,filtered_logits_2