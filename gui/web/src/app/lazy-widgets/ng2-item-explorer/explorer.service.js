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
var JsonNode_1 = require('./JsonNode');
var Ng2ItemExplorerService = (function () {
    function Ng2ItemExplorerService(restService) {
        var _this = this;
        this.testData = {
            first: "iamfirst",
            second: {
                "second_": "is",
                "an": "object"
            },
            third: 2,
            fourth: {
                nested: {
                    "hi": "hihihi"
                },
                "woop": "floop"
            },
            I_AM_AN_ARRAY: [
                { "an": "element" },
                { "another": "element" }
            ]
        };
        this.currentModelSubject = new Subject_1.Subject();
        this.currentModel = this.currentModelSubject.asObservable();
        this.modelNamesSubject = new Subject_1.Subject();
        this.modelNames = this.modelNamesSubject.asObservable();
        this.testNode = new JsonNode_1.JsonNode(this.testData);
        console.log(this.testNode);
        this.getNode = function (keys) {
            // this is where it should fetch nodes from restservice to make it lazy
            var data = _this.testData;
            for (var _i = 0, keys_1 = keys; _i < keys_1.length; _i++) {
                var key = keys_1[_i];
                data = data[key];
            }
            return data;
        };
        this.selectModel = function (name) {
            console.log("Selected model: " + name);
            _this.currentModelSubject.next(_this.getNode([])); // top level node
        };
        this.refresh = function () {
            restService.getModels().then(function (data) {
                _this.modelNamesSubject.next(data);
            });
        };
    }
    Ng2ItemExplorerService.prototype.ngOnInit = function () {
        this.refresh();
    };
    Ng2ItemExplorerService = __decorate([
        core_1.Injectable(),
        __param(0, core_1.Inject('restService')), 
        __metadata('design:paramtypes', [Object])
    ], Ng2ItemExplorerService);
    return Ng2ItemExplorerService;
}());
exports.Ng2ItemExplorerService = Ng2ItemExplorerService;
//# sourceMappingURL=explorer.service.js.map