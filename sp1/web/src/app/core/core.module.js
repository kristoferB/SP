(function () {
    'use strict';

    angular
        .module('app.core', [
            'ngAnimate', 'ngSanitize', 'gridster', 'ngStorage', 'ngJsTree', 'uuid4',
            'blocks.exception', 'blocks.logger', 'blocks.router', 'mwl.confirm', 'dialogs.main',
            'dialogs.default-translations', 'pascalprecht.translate', 'ui.router', 'smart-table', 'xeditable',
            'ui.bootstrap', 'angular.filter', 'nvd3', 'toColorFilter'
        ])
        .run(runBlock);

    runBlock.$inject = ['inlineEditingService'];

    function runBlock(inlineEditingService) {
        inlineEditingService.initialize();
    }

})();
