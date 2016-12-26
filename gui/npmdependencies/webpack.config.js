module.exports = {
    entry: [
        './vendor.js'
    ],
    output: {
        path: 'output/',
        filename: 'bundle.js'
    },
    module: {
        loaders: [
            { test: /\.css$/,
              loader: 'style!css?sourceMap'
            }, {
              test: /\.woff(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'url?limit=10000&mimetype=application/font-woff'
            }, {
              test: /\.woff2(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'url?limit=10000&mimetype=application/font-woff'
            }, {
              test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'url?limit=10000&mimetype=application/octet-stream'
            }, {
              test: /\.eot(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'file'
            }, {
              test: /\.svg(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'url?limit=10000&mimetype=image/svg+xml'
            }, {
              test: /\.png(\?v=\d+\.\d+\.\d+)?$/,
              loader: 'url?limit=100000'
            }
        ]
    }
};
