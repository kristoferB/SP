(function () {
    'use strict';

    angular
        .module('app.core', [
            'ngAnimate', 'ngSanitize', 'ngMaterial', 'mdDataTable',
            'blocks.exception', 'blocks.logger', 'blocks.router',
            'ui.router'
        ]);
})();
