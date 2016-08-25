// //import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';
import { Injectable, Inject } from "@angular/core";
import { Ng2DashboardService } from "../dashboard/ng2-dashboard.service";
import { Subject, Observable } from "rxjs/Rx";
import {DOCUMENT} from "@angular/platform-browser";

@Injectable()
export class ThemeService {

    storage: any;

    showHeaders: boolean;
    showNavbar: boolean;
    showWidgetOptions: boolean;

    normalView: () => void;
    compactView: () => void;
    maximizedContentView: () => void;

    toggleNavbar: () => void;

    enableEditorMode: () => void;
    disableEditorMode: () => void;

    editorModeEnabled: boolean;
    currentView: string;

    setColorTheme: (theme: string) => void;
    setLayoutTheme: (theme: string) => void;

    configureGridster: () => void;
    update: () => void;

    constructor(
        private ng2DashboardService: Ng2DashboardService, //,
        @Inject('$http') http,   // eventually use the ng2 http here
        @Inject(DOCUMENT) private document
    ) {
        // this does not need to exist; functional fo' life
        // this.storage = {
        //     gridsterConstants: {
        //         margin: 10
        //     },
        //     // by default, less is unchanged
        //     lessColorConstants: new Observable<Object>(),
        //     lessLayoutConstants: new Observable<Object>()
        // };

        this.showHeaders = true;
        this.showNavbar = true;
        this.showWidgetOptions = true;

        this.normalView = () => {
            this.currentView = "normalView";
            ng2DashboardService.setPanelMargins(10);
            this.showHeaders = true;
            this.setLayoutTheme("default");
        };

        this.compactView = () => {
            this.currentView = "compactView";
            ng2DashboardService.setPanelMargins(3);
            this.showHeaders = true;
            this.setLayoutTheme("compact");
        };

        this.maximizedContentView = () => {
            this.currentView = "maximizedContentView";
            ng2DashboardService.setPanelMargins(0);
            this.showHeaders = false;
            this.setLayoutTheme("maximized_content");
        };

        this.enableEditorMode = () => {
            this.editorModeEnabled = true;
            ng2DashboardService.setPanelLock(true);
            this.showWidgetOptions = true;
        };

        this.disableEditorMode =  () =>{
            this.editorModeEnabled = false;
            ng2DashboardService.setPanelLock(false);
            this.showWidgetOptions = false;
        };


        this.toggleNavbar = () => {
            this.showNavbar = !this.showNavbar;
            //this.update();
        };

        this.editorModeEnabled = true;
        this.currentView = "test";

        this.setColorTheme = (theme: string) =>  {
            // this.httpGet(
            //     "/style_presets/colors/" + theme,
            //     (res: Object) => {
            //     }
            // )
            this.document.getElementById('color_theme').setAttribute('href', '../.tmp/color/'+theme+'.css');
        };

        this.setLayoutTheme = (theme: string) => {
            // this.httpGet(
            //     "/style_presets/layouts/" + theme,
            //     (res: Object) => {
            //     }
            // );

            this.document.getElementById('layout_theme').setAttribute('href', '../.tmp/layout/'+theme+'.css');

        };

        //  function resetGrid(){ //TODO rewrite this
        //  var navbarHeight = 0;
        //  if(this.showNavbar){
        //  navbarHeight = 50;
        //  }
        //  dashboardService.gridsterOptions.rowHeight = (window.innerHeight-navbarHeight) / 8;
        //  setTimeout(resetGrid, 0.3);
        //  }
        //
        // this.httpGet = (url: string, callback: (res: string) => any) => {
        //     http.get(url, "json").
        //     then(function successCallback(response) {
        //             callback(response)
        //         }, function errorCallback(response) {
        //             console.log('http request errored');
        //             console.log(response);
        //         }
        //     );
        // };
        this.configureGridster = () => {
            //ng2DashboardService.setPanelMargins(this.storage.gridsterConstants.margin);
        };
    }
}
