(function () {
    'use strict';

    angular
      .module('app.itemEditor')
      .directive('jsonEditor', jsoneditor)
      .constant('jsoneditorConfig', {})

    jsoneditor.$inject = ['$compile', 'logger', 'jsoneditorConfig', '$timeout'];
    /* @ngInject */
    function jsoneditor($compile, logger, jsoneditorConfig, $timeout) {
        var defaults = jsoneditorConfig;
        var directive = {
            restrict: 'A',
            require: 'ngModel',
            scope: {'options': '=', 'jsonEditor': '=', 'preferText': '='},
            link: function ($scope, element, attrs, ngModel) {
                var editor;

                if (!angular.isDefined(window.JSONEditor)) {
                    throw new Error("Please add the jsoneditor.js script first!");
                }

                function _createEditor(options) {
                    var settings = angular.extend({}, defaults, options);
                    var theOptions = angular.extend({}, settings, {
                        change: function () {
                            $timeout(function () {
                                if (editor) {
                                    ngModel.$setViewValue($scope.preferText === true ? editor.getText() : editor.get());

                                    if (settings && settings.hasOwnProperty('change')) {
                                        settings.change();
                                    }
                                }
                            });
                        }
                    });

                    element.html('');

                    var instance = new JSONEditor(element[0], theOptions);

                    if ($scope.jsonEditor instanceof Function) {
                        $timeout(function () {
                            $scope.jsonEditor(instance);
                        });
                    }

                    return instance;
                }

                $scope.$watch('options', function (newValue, oldValue) {
                    for (var k in newValue) {
                        if (newValue.hasOwnProperty(k)) {
                            var v = newValue[k];

                            if (newValue[k] !== oldValue[k]) {
                                if (k === 'mode') {
                                    editor.setMode(v);
                                } else if (k === 'name') {
                                    editor.setName(v);
                                } else { //other settings cannot be changed without re-creating the JsonEditor
                                    editor = _createEditor(newValue);
                                    ngModel.$render();
                                    return;
                                }
                            }
                        }
                    }
                }, true);

                $scope.$on('$destroy', function () {
                    //remove jsoneditor?
                });

                ngModel.$render = function () {
                    if (($scope.preferText === true) && !angular.isObject(ngModel.$viewValue)) {
                        editor.setText(ngModel.$viewValue || '{}');
                    } else {
                        editor.set(ngModel.$viewValue || {});
                    }
                };

                editor = _createEditor($scope.options);
            }
        };

        return directive;
    }

})();

