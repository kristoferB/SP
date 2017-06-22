from googleapiclient import discovery
from googleapiclient import errors
from oauth2client.client import GoogleCredentials
from google.cloud import pubsub
import json

def request_prediction(body):
    project = 'double-carport-162512'
    model = 'edvards_test'
    version = 'v1'
    credentials = GoogleCredentials.get_application_default()
    print credentials.id_token

    service = discovery.build('ml', 'v1', credentials=credentials)
    name = 'projects/{}/models/{}/versions/{}'.format(project, model, version)
    request = service.projects().predict(
        name=name,
        body=body
    )
    response = request.execute()
    '''
    try:
        response = request.execute()
        print(response)
    except errors.HttpError, err:
        print('There was an error creating the model. Check the details:')
        print(err._get_reason())
    '''
    return response

topic_name = 'prediction-test'
subscription_name = 'request-listener'

client = pubsub.Client()
topic = client.topic(topic_name)
subscription = topic.subscription(subscription_name)

print('listening for messages')
while True:
    results = subscription.pull()
    #print([message.data.decode('utf-8') for ack_id, message in results])
    subscription.acknowledge([ack_id for ack_id, message in results])
    for _, message in results:
        try:
            print 'message data:'
            d = message.data
            d_as_json = json.loads(d)
            tfrequest = d_as_json['tfrequest']
            print tfrequest
            #dict = {'instances': [{'input_a': -1, 'input_b': 3}]}
            tfoutput = request_prediction(tfrequest)
            to_send = json.dumps({'tfoutput': tfoutput})
            topic.publish(to_send)
        except (AttributeError, ValueError, KeyError):
            print 'error caught by me'

    #print(request_prediction({'instances': [{'input_a': -1, 'input_b': 3}]})) # sooo this should only happen after receiving something
