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
var core_1 = require('@angular/core');
var AwesomeNG2Component = (function () {
    function AwesomeNG2Component() {
    }
    AwesomeNG2Component = __decorate([
        core_1.Component({
            selector: 'awesome-ng2-component',
            template: "\n    <h2>Hello from Angular2!</h2>\n    <div><label>id: </label>1</div>\n  "
        }), 
        __metadata('design:paramtypes', [])
    ], AwesomeNG2Component);
    return AwesomeNG2Component;
}());
exports.AwesomeNG2Component = AwesomeNG2Component;
//# sourceMappingURL=awesome-ng2-component.component.js.map