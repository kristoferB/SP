var React = require('react');
window.React = React;

var ReactDOM = require('react-dom');
window.ReactDOM = ReactDOM;

var ReactGridLayout = require('react-grid-layout');
window.ReactGridLayout = ReactGridLayout;
require('react-grid-layout/css/styles.css');

var JSONEditor = require('jsoneditor');
window.JSONEditor = JSONEditor;
require('jsoneditor/dist/jsoneditor.css');

window.GoogleChartsLoaded = false;
const script = (typeof window !== 'undefined') ? require('scriptjs') : null;
script("https://www.gstatic.com/charts/loader.js", () => {
    google.charts.load('current', {packages: ['corechart','gantt']});
    google.charts.setOnLoadCallback(() => {
        window.GoogleChartsLoaded = true;
        window.GoogleVisualization = google.visualization;
    });
});

var jQuery = require('jquery');
window.jQuery = jQuery;

var bootstrap = require('bootstrap');
window.bootstrap = bootstrap;
require('bootstrap/dist/css/bootstrap.css');
