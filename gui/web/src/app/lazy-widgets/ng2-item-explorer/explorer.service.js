"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var core_1 = require('@angular/core');
var Subject_1 = require('rxjs/Subject');
var event_bus_service_1 = require("../../core/event-bus.service");
var Ng2ItemExplorerService = (function () {
    function Ng2ItemExplorerService(restService, itemService, modelService, evBus) {
        var _this = this;
        this.currentModelSubject = new Subject_1.Subject();
        this.currentModel = this.currentModelSubject.asObservable();
        this.modelNamesSubject = new Subject_1.Subject();
        this.modelNames = this.modelNamesSubject.asObservable();
        this.activeModel = "";
        this.refresh = function () {
            restService.getModels().then(function (data) {
                _this.modelNamesSubject.next(data);
            });
        };
        this.getRoots = function (model) {
            console.log('filtering model: ');
            console.log(model);
            var roots = new Array();
            for (var _i = 0, model_1 = model; _i < model_1.length; _i++) {
                var element = model_1[_i];
                if (element['isa'] == 'HierarchyRoot') {
                    roots.push(element);
                }
            }
            return roots;
        };
        this.activeModel = modelService.activeModel;
        this.model = itemService.items;
        this.structures = this.getRoots(this.model);
        console.log("strucutres: ");
        console.log(this.structures);
        var idList = this.structures.map(function (x) { return x.id; });
        evBus.tweetToTopic("minTopic", idList);
    }
    Ng2ItemExplorerService.prototype.ngOnInit = function () {
        //this.refresh();	
    };
    Ng2ItemExplorerService = __decorate([
        core_1.Injectable(),
        __param(0, core_1.Inject('restService')),
        __param(1, core_1.Inject('itemService')),
        __param(2, core_1.Inject('modelService')), 
        __metadata('design:paramtypes', [Object, Object, Object, event_bus_service_1.EventBusService])
    ], Ng2ItemExplorerService);
    return Ng2ItemExplorerService;
}());
exports.Ng2ItemExplorerService = Ng2ItemExplorerService;
//# sourceMappingURL=explorer.service.js.map