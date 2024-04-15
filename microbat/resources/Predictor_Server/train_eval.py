import numpy as np
import torch
import logging
from tqdm import tqdm
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, classification_report

logging.basicConfig(format = '%(asctime)s - %(levelname)s - %(name)s -   %(message)s',
                    datefmt = '%m/%d/%Y %H:%M:%S',
                    level = logging.INFO)
logger = logging.getLogger(__name__)
def train_model(model,dataloader,optimizer,device,epoch):
    logger.info("****** Train model ******")
    model.train()

    # min_loss = float('inf')
    for e in range(epoch):
        logger.info(f"training epoch {e+1}")
        loss_epoch = 0
        for batch in tqdm(dataloader,desc = "training", leave = False):
            batch = tuple(t.to(device) for t in batch)
            source_ids,source_mask,target_ids = batch  
            loss1,loss2 = model(source_ids=source_ids,source_mask=source_mask,target_ids=target_ids,type="train")

            loss = loss1 + loss2
            loss_epoch += loss.item()
            
            loss.backward()

            optimizer.step()
            optimizer.zero_grad()

        logger.info(f"total loss in epoch {e+1}: {loss_epoch}")


label2id_1 = {'<ROOT>':0,'<CORRECT>':1,'<CONTROL>':2,'<DATA>':3}
label2id_2 = {'<TRUE>':0,'<FALSE>':1}

def eval_model(model,eval_dataloader,device,args):
    logger.info("****** Evaluate model ******")
    model.eval()
    pred_labels_1 = []
    gold_labels_1 = []

    pred_labels_2 = []
    gold_labels_2 = []

    for batch in tqdm(eval_dataloader,desc="evaluation",leave=False):
        source_ids,source_mask,target_ids=[b.to(device) for b in batch]

        with torch.no_grad():
            filtered_logits_1,filtered_logits_2 = model(source_ids,source_mask,target_ids,type="eval")
            # # eval for mask1
            active_loss_1 = (source_ids == model.mask1_id).contiguous().view(-1) # find which tokens are masked 1
            labels_ids1 = target_ids.contiguous().view(-1)[active_loss_1] # get the labels of the masked tokens
            labels_1 = torch.tensor([label2id_1[model.tokenizer.decode(x,clean_up_tokenization_spaces=False)] for x in labels_ids1]).to(device)
            # filtered_logits_1 = lm_logits_1.contiguous().view(-1, 4)[active_loss_1] # get the logits of the masked tokens

            # # eval for mask2
            data_mask = (labels_1 == 3).contiguous().view(-1)
            filtered_source_ids = source_ids[data_mask]
            filtered_target_ids = target_ids[data_mask]
            # filtered_lm_logits_2 = lm_logits_2[data_mask]

            active_loss_2 = (filtered_source_ids == model.mask2_id).contiguous().view(-1) # find which tokens are masked 1
            labels_ids2 = filtered_target_ids.contiguous().view(-1)[active_loss_2] # get the labels of the masked tokens
            labels_2 = torch.tensor([label2id_2[model.tokenizer.decode(x,clean_up_tokenization_spaces=False)] for x in labels_ids2]).to(device)
            # filtered_logits_2 = filtered_lm_logits_2.contiguous().view(-1, 2)[active_loss_2] # get the logits of the masked tokens


        pred_labels_1.append(torch.argmax(filtered_logits_1,dim=-1))
        gold_labels_1.append(labels_1)
        pred_labels_2.append(torch.argmax(filtered_logits_2,dim=-1))
        gold_labels_2.append(labels_2)
    
    #==========================================================================
    pred_labels_1 = torch.cat(pred_labels_1,dim=0).cpu().numpy()
    gold_labels_1 = torch.cat(gold_labels_1,dim=0).cpu().numpy()
    acc = accuracy_score(gold_labels_1, pred_labels_1)
    precision = precision_score(gold_labels_1, pred_labels_1, average='macro', zero_division=0)
    recall = recall_score(gold_labels_1, pred_labels_1, average='macro', zero_division=0)
    f1 = f1_score(gold_labels_1, pred_labels_1, average='macro', zero_division=0)
    result = {  'Accuracy': round(np.mean(acc),4),
                'Precision': round(np.mean(precision),4),
                'Recall': round(np.mean(recall),4),
                'F1': round(np.mean(f1),4)
                }
    # report = classification_report(gold_labels_1, pred_labels_1, digits=4, zero_division=0)
    # logger.info(f"Classification report for MASK1:\n{report}")
    logger.info("  [Evaluation for MASK1]")
    for key in result.keys():
        logger.info("  %s = %s", key, str(result[key]))
    logger.info("  "+"*"*20)

    #==========================================================================
    pred_labels_2 = torch.cat(pred_labels_2,dim=0).cpu().numpy()
    gold_labels_2 = torch.cat(gold_labels_2,dim=0).cpu().numpy()
    acc_2 = accuracy_score(gold_labels_2, pred_labels_2)
    precision_2 = precision_score(gold_labels_2, pred_labels_2, average='macro', zero_division=0)
    recall_2 = recall_score(gold_labels_2, pred_labels_2, average='macro', zero_division=0)
    f1_2 = f1_score(gold_labels_2, pred_labels_2, average='macro', zero_division=0)
    result_2 = {  'Accuracy': round(np.mean(acc_2),4),
                'Precision': round(np.mean(precision_2),4),
                'Recall': round(np.mean(recall_2),4),
                'F1': round(np.mean(f1_2),4)
                }
    # report_2 = classification_report(gold_labels_2, pred_labels_2, digits=4, zero_division=0)
    # logger.info(f"Classification report for MASK2:\n{report_2}")
    logger.info("  [Evaluation for MASK2]")
    for key in result_2.keys():
        logger.info("  %s = %s", key, str(result_2[key]))
    logger.info("  "+"*"*20)

    #==========================================================================
    pred_labels_1_root = []
    gold_labels_1_root = []
    for x in range(len(gold_labels_1)):
        pred_labels_1_root.append(1 if pred_labels_1[x] == 0 else 0)
        gold_labels_1_root.append(1 if gold_labels_1[x] == 0 else 0)

    acc= accuracy_score(gold_labels_1_root, pred_labels_1_root)
    precision = precision_score(gold_labels_1_root, pred_labels_1_root, average='macro', zero_division=0)
    recall = recall_score(gold_labels_1_root, pred_labels_1_root, average='macro', zero_division=0)
    f1 = f1_score(gold_labels_1_root, pred_labels_1_root, average='macro', zero_division=0)
    result = {  'Accuracy': round(np.mean(acc),4),
                'Precision': round(np.mean(precision),4),
                'Recall': round(np.mean(recall),4),
                'F1': round(np.mean(f1),4)
                }
    logger.info("  [Evaluation for <ROOT>]")
    for key in result.keys():
        logger.info("  %s = %s", key, str(result[key]))
    logger.info("  "+"*"*20)

    #==========================================================================
    pred_labels_1_correct = []
    gold_labels_1_correct = []
    for x in range(len(gold_labels_1)):
        pred_labels_1_correct.append(1 if pred_labels_1[x] == 1 else 0)
        gold_labels_1_correct.append(1 if gold_labels_1[x] == 1 else 0)

    acc = accuracy_score(gold_labels_1_correct, pred_labels_1_correct)
    precision = precision_score(gold_labels_1_correct, pred_labels_1_correct, average='macro', zero_division=0)
    recall = recall_score(gold_labels_1_correct, pred_labels_1_correct, average='macro', zero_division=0)
    f1 = f1_score(gold_labels_1_correct, pred_labels_1_correct, average='macro', zero_division=0)
    result = {  'Accuracy': round(np.mean(acc),4),
                'Precision': round(np.mean(precision),4),
                'Recall': round(np.mean(recall),4),
                'F1': round(np.mean(f1),4)
                }
    logger.info("  [Evaluation for <CORRECT>]")
    for key in result.keys():
        logger.info("  %s = %s", key, str(result[key]))
    logger.info("  "+"*"*20)

    #==========================================================================
    pred_labels_2_false = []
    gold_labels_2_false = []
    for x in range(len(gold_labels_1)):
        pred_labels_2_false.append(1 if pred_labels_2[x] == 1 else 0)
        gold_labels_2_false.append(1 if gold_labels_2[x] == 1 else 0)

    acc = accuracy_score(gold_labels_2_false, pred_labels_2_false)
    precision = precision_score(gold_labels_2_false, pred_labels_2_false, average='macro', zero_division=0)
    recall = recall_score(gold_labels_2_false, pred_labels_2_false, average='macro', zero_division=0)
    f1 = f1_score(gold_labels_2_false, pred_labels_2_false, average='macro', zero_division=0)
    result = {  'Accuracy': round(np.mean(acc),4),
                'Precision': round(np.mean(precision),4),
                'Recall': round(np.mean(recall),4),
                'F1': round(np.mean(f1),4)
                }
    logger.info("  [Evaluation for <FALSE>]")
    for key in result.keys():
        logger.info("  %s = %s", key, str(result[key]))
    logger.info("  "+"*"*20)