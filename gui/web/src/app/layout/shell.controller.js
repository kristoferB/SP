(function() {
    'use strict';

    angular
        .module('app.layout')
        .controller('ShellController', ShellController);

    ShellController.$inject = ['config', 'logger', '$document'];
    /* @ngInject */
    function ShellController(config, logger, $document) {
        var vm = this;
        vm.navline = {
            title: config.appTitle
        };

        activate();

        function activate() {
            giveFeedbackOnDropTargets();
            logger.success('Shell Controller: ' + config.appTitle + ' loaded!', null);
        }

        function giveFeedbackOnDropTargets() {
            $document.bind('dnd_move.vakata', onMove);

            function onMove(e, data) {
                var t = angular.element(data.event.target);
                if(!t.closest('.jstree').length) {
                    if(t.closest('[drop-target]').length) {
                        data.helper.find('.jstree-icon').removeClass('jstree-er').addClass('jstree-ok');
                    }
                    else {
                        data.helper.find('.jstree-icon').removeClass('jstree-ok').addClass('jstree-er');
                    }
                }
            }
        }

    }
})();
