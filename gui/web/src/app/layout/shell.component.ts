import { Component, Inject } from '@angular/core';

import { upgAdapter } from '../upg-helpers/upg-adapter';
import { SpTopNavComponent } from './sp-top-nav.component';
import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';

@Component({
  selector: 'shell',
  templateUrl: 'app/layout/shell.component.html',
  styleUrls: [],
  directives: [SpTopNavComponent,
            upgAdapter.upgradeNg1Component('upgUiView')],
  providers: [Ng2DashboardService]
})

// TODO test this whole class, does it do what the original controller did??
export class ShellComponent {

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
