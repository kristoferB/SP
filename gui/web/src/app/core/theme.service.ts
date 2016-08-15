// //import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';
import {Injectable, Inject} from "@angular/core";
import {Ng2DashboardService} from "../dashboard/ng2-dashboard.service";
import {Subject, Observable} from "rxjs/Rx";

@Injectable()
export class ThemeService {

    storage: any;

    showHeaders: boolean;
    showNavbar: boolean;
    showWidgetOptions: boolean;

    normalView: () => void;
    compactView: () => void;
    maximizedContentView: () => void;

    toggleNavbar:  () => void;

    enableEditorMode: () => void;
    disableEditorMode: () => void;

    editorModeEnabled: boolean;
    currentView: string;

    setColorTheme: (theme: string) => void;
    setLayoutTheme: (theme: string) => void;

    compileLess: () => void;
    configureGridster: () => void;
    update: () => void;

    testColor: Observable<string>;
    testSubject: Subject<string>;

    coolSubscribe: (callback: (change: string) => any) => void;

    constructor(
        private ng2DashboardService: Ng2DashboardService, //,
        @Inject('$http') http   // eventually use the ng2 http here
    ) {
        this.coolSubscribe = (callback: (change: string) => any) => {
            this.testSubject.subscribe(callback)
        };

        this.testSubject = new Subject<any>();
        this.testColor = this.testSubject.asObservable();

        this.storage = {
            gridsterConstants: {
                margin: 10
            },
            // by default, less is unchanged
            lessColorConstants: new Observable<Object>(),
            lessLayoutConstants: new Observable<Object>()
        };

        this.showHeaders = true;
        this.showNavbar = true;
        this.showWidgetOptions = true;

        this.normalView = () => {
            this.testSubject.next("blue");

            this.currentView = "normalView";
            this.storage.gridsterConstants.margin = 10;
            this.showHeaders = true;
            this.setLayoutTheme("normalView");
        };

        this.compactView = () => {
            this.testSubject.next("green");

            this.currentView = "compactView";
            this.storage.gridsterConstants.margin = 3;
            this.showHeaders = true;
            this.setLayoutTheme("compactView");
        };

        this.maximizedContentView = () =>{
            this.currentView = "maximizedContentView";
            this.storage.gridsterConstants.margin = 0;
            this.showHeaders = false;
            this.setLayoutTheme("maximizedContentView");
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


        this.toggleNavbar =  () => {
            this.showNavbar = !this.showNavbar;
            this.update();
        };

        this.editorModeEnabled = true;
        this.currentView = "test";

        this.setColorTheme = function (theme) {
            this.httpGet(
                "/style_presets/colors/"+theme,
                (res: Object) => {
                    this.storage.lessColorConstants = res;
                    this.update();
                }
            )
        };

        this.setLayoutTheme = function(theme) {
            httpGet(
                "/style_presets/layouts/"+theme,
                (res: Object) => {
                    this.storage.lessLayoutConstants = res;
                    this.update();
                }
            );
        };

        //  function resetGrid(){ //TODO rewrite this
        //  var navbarHeight = 0;
        //  if(this.showNavbar){
        //  navbarHeight = 50;
        //  }
        //  dashboardService.gridsterOptions.rowHeight = (window.innerHeight-navbarHeight) / 8;
        //  setTimeout(resetGrid, 0.3);
        //}

        this.configureGridster = () => {
            ng2DashboardService.setPanelMargins(this.storage.gridsterConstants.margin);
        };


        this.compileLess = () => {
            //merge config variables into the .less file
            console.log('recompile less');

            // less.modifyVars(
            //     Object.assign(
            //         this.storage.lessColorConstants,
            //         this.storage.lessLayoutConstants,
            //         {showNavbar: this.showNavbar}
            //     )
            // );
        };

        this.update = () => {
            this.compileLess();
            this.configureGridster();
        };

        function httpGet(url: string, callback: (res: string) => any){
            http.get(url, "json").
            then(function successCallback(response) {
                    callback(response)
                }, function errorCallback(response) {
                    console.log('http request errored');
                    console.log(response);
                }
            );
        }

        this.compileLess();
    }
}




