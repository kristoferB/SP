/**
 * Created by daniel on 2015-08-11.
 */
(function () {
    'use strict';

    angular
        .module('app.itemList')
        .controller('ItemListController', ItemListController);

    ItemListController.$inject = ['logger'];
    /* @ngInject */
    function ItemListController(logger) {
        var vm = this;
        vm.json =  {
            "Array": [1, 2, 3],
            "Boolean": true,
            "Null": null,
            "Number": 123,
            "Object": {
                "a": "b",
                "c": "d"
            },
            "String": "Hello World",
            "String2": "Hello World 2",
            "String3": "Hello World 3",
            "String4": "Hello World 4"
        };
        vm.options = {
            mode: 'tree',
            modes: [
                'tree',
                'view',
                'form',
                'code',
                'text'
            ]
        };

        activate();

        function activate() {
            logger.info('Added a Item List widget');
        }
    }
})();
