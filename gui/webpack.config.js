'use strict';

var webpack = require('webpack');
var path = require('path');

module.exports = {
    output: {
        path: __dirname + '/build',
        publicPath: __dirname  + "/build/",
        filename: 'bundle.js'
    }
};

