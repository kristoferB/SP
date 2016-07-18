import { Component, Inject } from '@angular/core';

import { upgradeAdapter } from '../upgrade_adapter';
import { Ng2HtTopNavComponent } from './ng2-ht-top-nav.component';

@Component({
  selector: 'ng2-shell',
  templateUrl: 'app/layout/shell.html',
  styleUrls: [],
  directives: [Ng2HtTopNavComponent,
            upgradeAdapter.upgradeNg1Component('upgUiView')],
  providers: []
})

// TODO test this whole class, does it do what the original controller did??
export class Ng2ShellComponent {

    angular: any;

    navline: any;

    config: any;
    logger: any;
    $document: any;
    settingsService: any;

    vm: any = {}; // TODO

    constructor(
        @Inject('config') config,
        @Inject('logger') logger,
        @Inject('$document') $document,
        @Inject('settingsService') settingsService
        ) {
            this.navline = { title: config.appTitle };

            this.config = config;
            this.logger = logger;
            this.$document = $document;
            this.settingsService = settingsService; // TODO
            this.vm.settingsService = settingsService; // TODO
            
            this.activate();
    }

    private activate() {
        this.giveFeedbackOnDropTargets();
        this.logger.log('Shell Controller: ' + this.config.appTitle +
                        ' loaded!', null);
    }


    giveFeedbackOnDropTargets() {
        this.$document.bind('dnd_move.vakata', onMove);

        function onMove(e, data) {
            var t = this.angular.element(data.event.target);
            if(!t.closest('.jstree').length) {
                if(t.closest('[drop-target]').length) {
                    data.helper.find('.jstree-icon')
                        .removeClass('jstree-er').addClass('jstree-ok');
                }
                else {
                    data.helper.find('.jstree-icon')
                        .removeClass('jstree-ok').addClass('jstree-er');
                }
            }
        }
    }
}
