var React = require('react');
window.React = React;

var ReactDOM = require('react-dom');
window.ReactDOM = ReactDOM;

var ReactGridLayout = require('react-grid-layout');
window.WidthProvider = ReactGridLayout.WidthProvider;
window.ResponsiveReactGridLayout = WidthProvider(ReactGridLayout.Responsive);
require('react-grid-layout/css/styles.css');

var JSONEditor = require('jsoneditor');
window.JSONEditor = JSONEditor;
require('jsoneditor/dist/jsoneditor.css');

var jQuery = require('jquery');
window.jQuery = jQuery;

window.GoogleChartsLoaded = false;
const script = (typeof window !== 'undefined') ? require('scriptjs') : null;
script("https://www.gstatic.com/charts/loader.js", () => {
    google.charts.load('current', {packages: ['corechart','gantt','timeline']});
    google.charts.setOnLoadCallback(() => {
        window.GoogleChartsLoaded = true;
        window.GoogleVisualization = google.visualization;
    });
});

var bootstrap = require('bootstrap');
window.bootstrap = bootstrap;
require('bootstrap/dist/css/bootstrap.css');
require('bootstrap/fonts/glyphicons-halflings-regular.ttf');
require('bootstrap/fonts/glyphicons-halflings-regular.eot');
require('bootstrap/fonts/glyphicons-halflings-regular.svg');

require('font-awesome/css/font-awesome.css');
