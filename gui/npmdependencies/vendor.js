var React = require('react');
window.React = React;

var ReactDOM = require('react-dom');
window.ReactDOM = ReactDOM;

var ReactGridLayout = require('react-grid-layout');
window.WidthProvider = ReactGridLayout.WidthProvider;
window.ResponsiveReactGridLayout = WidthProvider(ReactGridLayout.Responsive);
window.ReactGridLayout = ReactGridLayout;
require('react-grid-layout/css/styles.css');


var JSONEditor = require('jsoneditor');
window.JSONEditor = JSONEditor;
require('jsoneditor/dist/jsoneditor.css');

var jQuery = require('jquery');
window.jQuery = jQuery;

var bootstrap = require('bootstrap');
window.bootstrap = bootstrap;
require('bootstrap/dist/css/bootstrap.css');
require('bootstrap/fonts/glyphicons-halflings-regular.ttf');
require('bootstrap/fonts/glyphicons-halflings-regular.eot');
require('bootstrap/fonts/glyphicons-halflings-regular.svg');

require('font-awesome/css/font-awesome.css');

var ReactDraggable = require('react-draggable');
window.ReactDraggable = ReactDraggable;

var angular = require('angular');
window.angular = angular;
require('angular-gantt');
require('angular-gantt/assets/angular-gantt.css');
require('angular-gantt/assets/angular-gantt-plugins.js');
require('angular-gantt/assets/angular-gantt-plugins.css');
require('angular-moment');
