var webpack = require('webpack');

var PROD = (process.env.NODE_ENV === 'production');

module.exports = {
    entry: [
        './vendor.js'
    ],
    output: {
        path: 'output/',
        filename: PROD ? 'bundle.min.js' : 'bundle.js'
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
    },
    plugins: PROD ? [
      new webpack.optimize.UglifyJsPlugin({
        minimize: true,
        mangle: true,
        compressor: { warnings: false }
      }),
      new webpack.DefinePlugin({
        'process.env': {
          // setting this again here, cause react needs it this way to
          // generate a real build-version of itself
          'NODE_ENV': JSON.stringify('production')
        }
    }),
    ] : []
};
