"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
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
var ItemExplorerNodeComponent = (function () {
    function ItemExplorerNodeComponent() {
    }
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Array)
    ], ItemExplorerNodeComponent.prototype, "children", void 0);
    ItemExplorerNodeComponent = __decorate([
        core_1.Component({
            selector: 'explorer-node',
            templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
            directives: [ItemExplorerNodeComponent]
        }), 
        __metadata('design:paramtypes', [])
    ], ItemExplorerNodeComponent);
    return ItemExplorerNodeComponent;
}());
exports.ItemExplorerNodeComponent = ItemExplorerNodeComponent;
var Node = (function () {
    function Node(name) {
        this.type = "undefined";
        //create observable
        // id: string;
        this.children = [];
        this.isValue = false;
        this.name = name;
    }
    return Node;
}());
exports.Node = Node;
var JSONObject = (function (_super) {
    __extends(JSONObject, _super);
    function JSONObject(name, json) {
        _super.call(this, name);
        this.type = "JSONObject";
        console.log(name + ": " + json);
        for (var key in json) {
            var child = json[key];
            if (Array.isArray(child)) {
                this.children.push(new JSONArray(key, child));
            }
            else if (typeof (child) == 'object') {
                this.children.push(new JSONObject(key, child));
            }
            else {
                this.children.push(new JSONValue(key, child));
            }
        }
    }
    return JSONObject;
}(Node));
exports.JSONObject = JSONObject;
var JSONArray = (function (_super) {
    __extends(JSONArray, _super);
    function JSONArray(name, json) {
        console.log(name + ": " + json);
        _super.call(this, name);
        this.type = "JSONArray";
        var i = 0;
        for (var _i = 0, json_1 = json; _i < json_1.length; _i++) {
            var child = json_1[_i];
            this.children.push(new JSONObject(i.toString(), child));
            i += 1;
        }
    }
    return JSONArray;
}(Node));
exports.JSONArray = JSONArray;
var JSONValue = (function (_super) {
    __extends(JSONValue, _super);
    function JSONValue(name, json) {
        _super.call(this, name);
        this.type = "JSONValue";
        this.isValue = true;
        console.log(name + ": " + json);
        this.value = json;
    }
    return JSONValue;
}(Node));
exports.JSONValue = JSONValue;
//# sourceMappingURL=explorer-node.component.js.map