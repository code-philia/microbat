import threading
from flask import Flask, request
import numpy as np
import torch
import logging
from torch.utils.data import DataLoader, TensorDataset

from model import mlmodel
from train_eval import eval_model, train_model
from transformers import AutoModel, AutoTokenizer, AutoConfig
# from transformers import (RobertaTokenizer,T5Config, T5ForConditionalGeneration)

logging.basicConfig(format = '%(asctime)s - %(levelname)s - %(name)s -   %(message)s',
                    datefmt = '%m/%d/%Y %H:%M:%S',
                    level = logging.INFO)
logger = logging.getLogger(__name__)

# MODEL_CLASSES = {'codeT5': (T5Config, T5ForConditionalGeneration, RobertaTokenizer)}
MODEL_CLASSES = {'codeT5': (AutoConfig, AutoModel, AutoTokenizer)}
pretrained_model_path = 'codet5p-220m-bimodal'
load_model_path = './checkpoint/codet5p_220m_1.bin'


Prediction_dic1 = ["<ROOT>", "<CORRECT>", "<CONTROL>","<DATA>"]
Prediction_dic2 = ["<TRUE>", "<FALSE>"]

"""
Set up environment
"""
device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

config_class, model_class, tokenizer_class = MODEL_CLASSES['codeT5']
config = config_class.from_pretrained(pretrained_model_path,trust_remote_code=True)
tokenizer = tokenizer_class.from_pretrained(pretrained_model_path,do_lower_case=True,trust_remote_code=True)
codeT5 = model_class.from_pretrained(pretrained_model_path,trust_remote_code=True)
encoder = codeT5.encoder

logger.info("load pretrained model done")

# add special tokens
new_special_tokens = ['<func>','</func>','<comment>','</comment>','<code_window>','</code_window>',
                      '<variables>','</variables>','<observed>','</observed>',
                      '<before>','<after>','<last_step>','<type>','<wrong_variable>',
                      '<MASK1>','<MASK2>','<ROOT>','<CORRECT>','<CONTROL>','<DATA>','<TRUE>','<FALSE>']

tokenizer.add_tokens(new_special_tokens, special_tokens=True)
encoder.resize_token_embeddings(len(tokenizer))
config.vocab_size = len(tokenizer)

mask1_id = tokenizer.convert_tokens_to_ids("<MASK1>")
mask2_id = tokenizer.convert_tokens_to_ids("<MASK2>")

logger.info("load tokenizer done")

model=mlmodel(encoder=encoder, config=config,tokenizer=tokenizer,device=device,use_focal_loss=True,
                beam_size=10, max_length=512,
                sos_id=tokenizer.cls_token_id, eos_id=tokenizer.sep_token_id, 
                mask1_id=mask1_id,mask2_id=mask2_id)

model.load_state_dict(torch.load(load_model_path))
model.to(device)
logger.info("load model done")

app1 = Flask('app1')
@app1.route('/predict', methods=['POST'])
def predict():
    data = request.get_json()
    source_seq = data['source_seq']
    # logger.info('source_seq: ' + source_seq)

    encoded_source_seq = tokenizer(source_seq, padding="max_length", truncation=True, max_length=512)
    source_ids = torch.tensor([encoded_source_seq["input_ids"]]).to(device)
    source_mask = torch.tensor([encoded_source_seq["attention_mask"]]).to(device)
    logist1,logist2 = model(source_ids,source_mask,None,"infer")

    step_pred_vec = logist1[0].tolist()
    step_pred = (int)(np.argmax(step_pred_vec,axis=-1))

    var_pred_vecs = logist2.tolist()
    var_preds = []
    for var_vec in var_pred_vecs:
        var_preds.append((int)(np.argmax(var_vec,axis=-1)))

    logger.info(step_pred_vec)
    logger.info(var_preds)

    return {
        'step_pred_vec': step_pred_vec, # []
        'step_pred':step_pred, # 0~3
        'var_pred_vecs':var_pred_vecs, # [[],[],[]]
        'var_preds':var_preds # 0~1
    }


mask1_ids = tokenizer.convert_tokens_to_ids(["<ROOT>","<CORRECT>","<CONTROL>","<DATA>"])
mask2_ids = tokenizer.convert_tokens_to_ids(["<TRUE>","<FALSE>"])

app2 = Flask('app2')
@app2.route('/learn', methods=['POST'])
def learn():
    data = request.get_json()
    target_seq = data['target_seq']
    # logger.info('target_seq: ' + target_seq)

    encoded_target_seq = tokenizer(target_seq, padding="max_length", truncation=True, max_length=512)
    target_ids = encoded_target_seq["input_ids"]

    source_ids = target_ids.copy()
    found_mask1 = False
    for i in range(0,len(source_ids)):
        if source_ids[i] in mask1_ids and found_mask1==False:
            source_ids[i] = mask1_id
            found_mask1 = True
            continue
        if source_ids[i] in mask2_ids:
            source_ids[i] = mask2_id

    target_ids = torch.tensor([encoded_target_seq["input_ids"]]).to(device)
    target_mask = torch.tensor([encoded_target_seq["attention_mask"]]).to(device)
    source_ids = torch.tensor([source_ids]).to(device)
    
    dataset = TensorDataset(source_ids,target_mask,target_ids)
    dataloader = DataLoader(dataset,batch_size=1,shuffle=False)
    optimizer = torch.optim.Adam(model.parameters(),lr=0.001)
    train_model(model=model,dataloader=dataloader,optimizer=optimizer,device=device,epoch=3)
    logger.info("train done")
    return {
        "state":"true"
    }

if __name__ == '__main__':
    # app1.run(host='127.0.0.1', port=5000)
    threading.Thread(target=app1.run, kwargs={'host':'127.0.0.1','port':5000}).start()
    threading.Thread(target=app2.run, kwargs={'host':'127.0.0.1','port':5001}).start()
    
