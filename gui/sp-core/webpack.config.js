'use strict';

var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: [
        './index.js'
    ],
    output: {
        path: __dirname + '/build',
        publicPath: __dirname  + "/build/",
        filename: 'bundle.js'
    }
};

