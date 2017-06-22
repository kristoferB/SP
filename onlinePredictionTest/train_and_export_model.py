import tensorflow as tf
import numpy as np
from tensorflow.contrib.learn import LinearRegressor
from tensorflow.contrib import layers
from tensorflow.contrib.learn.python.learn.utils import input_fn_utils

FEATURES = ["input_a", "input_b"]

def input_fn_train():
    feature_cols = {
        "input_a": tf.constant([[1], [2], [3]]),
        "input_b": tf.constant([[0], [-7], [4]])
    }
    outputs = tf.constant([-1, 0, 17])
    return feature_cols, outputs


feature_cols = [layers.real_valued_column(name) for name in FEATURES]
regressor = LinearRegressor(feature_columns=feature_cols, model_dir="./modeldir")
regressor.fit(input_fn=input_fn_train, steps=50)

def serving_input_fn():
    default_inputs = {col.name: tf.placeholder(col.dtype, [None]) for col in feature_cols}
    features = {key: tf.expand_dims(tensor, -1) for key, tensor in default_inputs.items()}
    return input_fn_utils.InputFnOps(
        features=features,
        labels=None,
        default_inputs=default_inputs
    )

regressor.export_savedmodel(
   "exportedmodel",
    serving_input_fn
)
