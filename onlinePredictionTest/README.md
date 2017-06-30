## This example was/is set up and run with the following steps ##
1. Install the python project:
```
virtualenv -p /usr/bin/python2.7 onlinePredictionTestEnv
source onlinePredictionTestEnv/bin/activate
pip install -r requirements.txt
```
2. Run the script that exports a trained model:
```
python train_and_export_model.py
```
3. Open the google cloud console in a browser. Upload the exported folder, named something like `exportedmodel/1498119228618/`, to a bucket in google cloud storage. Then go to ML engine, create the model `edvards_test`, and inside it the version `v1`, specifying the uploaded folder as source. Create the pubsub-topic `prediction-test` and create the subscriptions `request-listener` and `output-listener` to it.
4. Turn on the python service that handles prediction requests thrown at the gpubsub:
```
python prediction_requester.py
```
Then open a new terminal and run the scala code that requests a prediction:
```
cd ScalaRequestingPredViaPubsub
sbt run
```
