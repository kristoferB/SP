//(function() {
//    'use strict';
//
//    angular
//        .module('app')
//        .controller('ShellController', ShellController);
//
//    ShellController.$inject = ['config', 'logger', '$document', 'settingsService'];
//    /* @ngInject */
//    function ShellController(config, logger, $document, settingsService) {
//        var vm = this;
//        vm.navline = {
//            title: config.appTitle
//        };
//        vm.settingsService = settingsService;
//
//        activate();
//
//        function activate() {
//            giveFeedbackOnDropTargets();
//            logger.log('Shell Controller: ' + config.appTitle + ' loaded!', null);
//        }
//
//        function giveFeedbackOnDropTargets() {
//            $document.bind('dnd_move.vakata', onMove);
//
//            function onMove(e, data) {
//                var t = angular.element(data.event.target);
//                if(!t.closest('.jstree').length) {
//                    if(t.closest('[drop-target]').length) {
//                        data.helper.find('.jstree-icon').removeClass('jstree-er').addClass('jstree-ok');
//                    }
//                    else {
//                        data.helper.find('.jstree-icon').removeClass('jstree-ok').addClass('jstree-er');
//                    }
//                }
//            }
//        }
//
//    }
//})();
