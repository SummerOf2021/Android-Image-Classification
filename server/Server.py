import flask
import werkzeug
from keras.models import load_model
from keras.preprocessing import image
from keras.applications.imagenet_utils import preprocess_input, decode_predictions


import numpy as np


app = flask.Flask(__name__)


model = load_model('model_resnet.h5')





def model_predict(img_path, model):
    img = image.load_img(img_path, target_size=(224, 224))

    # Preprocessing the image
    x = image.img_to_array(img)
    # x = np.true_divide(x, 255)
    x = np.expand_dims(x, axis=0)

    # Be careful how your trained model deals with the input
    # otherwise, it won't make correct prediction!
    x = preprocess_input(x, mode='caffe')

    preds = model.predict(x)
    return preds
model_predict('predict.jpg', model)
    
    
@app.route('/', methods = ['POST','GET'])
def handle_request():
    imagefile = flask.request.files['image']
    filename = werkzeug.utils.secure_filename(imagefile.filename)
    imagefile.save(filename)
    preds = model_predict(filename, model)
    pred_class = decode_predictions(preds, top=1)
    result = str(pred_class[0][0][1])
    if (flask.request.headers.get("Authorization") == "82CC72"):
        return result
    return "Unauthorized"

app.run(host="0.0.0.0", port=5000, debug=True)

