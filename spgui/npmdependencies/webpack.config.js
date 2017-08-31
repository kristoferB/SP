var webpack = require('webpack');

var PROD = (process.env.NODE_ENV === 'production');

module.exports = {
    entry: [
        './vendor.js'
    ],
    output: {
        publicPath: './gui/npmdependencies/output/',
        path: 'output/',
        filename: PROD ? 'bundle.min.js' : 'bundle.js'
    },
    module: {
        loaders: [
            {
                test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
                loader: 'url-loader?limit=100000'
            }, {
                test: /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'url-loader?limit=100000'
            }, {
                test: /\.css(\?v=\d+\.\d+\.\d+)?$/,
                loader: "style-loader!css-loader"
            }, {
                test: /\.png(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'url-loader?limit=100000'
            }, {
		test: /\.json$/,
		loader: 'json-loader'
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
    ] : [
	// ignores 3 warnings, that are probably irrelevant, when webpacking
	// new webpack.IgnorePlugin(/regenerator|nodent|js\-beautify/, /ajv/)
    ]
};
