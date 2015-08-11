(function () {
    'use strict';

    angular
        .module('app.core', [
            'ngAnimate', 'ngSanitize', 'gridster', 'ngStorage', 'ng.jsoneditor',
            'blocks.exception', 'blocks.logger', 'blocks.router',
            'ui.router', 'smart-table', 'xeditable', 'ui.bootstrap'
        ])
        .run(runBlock);

    runBlock.$inject = ['inlineEditingService'];

    function runBlock(inlineEditingService) {
        inlineEditingService.initialize();
    }

})();
