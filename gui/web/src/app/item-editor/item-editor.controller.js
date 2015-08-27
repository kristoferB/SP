/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemEditor')
        .controller('ItemEditorController', ItemEditorController);

    ItemEditorController.$inject = ['logger', '$timeout'];
    /* @ngInject */
    function ItemEditorController(logger, $timeout) {
        var vm = this;
        vm.editor = null;
        vm.editorLoaded = function(editorInstance) {vm.editor = editorInstance;};
        vm.setMode = setMode;
        vm.modes = ['tree', 'view', 'form', 'code', 'text'];
        vm.search = function() {vm.editor.search(vm.editor.searchBox.dom.search.value);};
        vm.json =  {
            'Array': [1, 2, 3],
            'Boolean': true,
            'Null': null,
            'Number': 123,
            'Object': {
                'a': 'b',
                'c': 'd'
            },
            'String': 'Hello World',
            'String2': 'Hello World 2',
            'String3': 'Hello World 3',
            'String4': 'Hello World 4'
        };
        vm.options = {
            mode: 'tree'
        };
        vm.numberOfErrors = 0;

        activate();

        function activate() {

        }

        function setMode(mode) {
            vm.options.mode = mode;
            if (mode === 'code') {
                $timeout(function() {
                    vm.editor.editor.setOptions({maxLines: Infinity});
                    vm.editor.editor.on('change', function() {
                        $timeout(function() {
                            vm.numberOfErrors = vm.editor.editor.getSession().getAnnotations().length;
                        }, 300);
                    });
                });
            }
        }
    }
})();
