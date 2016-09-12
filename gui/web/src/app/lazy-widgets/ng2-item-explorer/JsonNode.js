"use strict";
var JsonNode = (function () {
    function JsonNode(json) {
        if (Array.isArray(json)) {
            this.value = new Array();
            this.type = 'array'; //  'array' in not a type in javascript (it's just an object)
            var i = 0;
            for (var _i = 0, _a = json; _i < _a.length; _i++) {
                var thing = _a[_i];
                this.value.push(new JsonNode(thing));
            }
        }
        else if (typeof (json) == 'object') {
            this.value = new Array();
            this.type = 'object';
            for (var myKey in json) {
                this.value.push([myKey, new JsonNode(json[myKey])]);
            }
        }
        else {
            this.value = json;
            this.type = typeof (json);
        }
    }
    return JsonNode;
}());
exports.JsonNode = JsonNode;
//# sourceMappingURL=JsonNode.js.map