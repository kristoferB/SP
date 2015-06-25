'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sse
 * @description
 * # sse
 * Factory in the spGuiApp.
 */
angular.module("spGuiApp")
.factory('sse', function ($rootScope) {
    var factory = {};

    factory.addSSEListener = function(eventName, callback) {
      console.log("Adding an SSE listener");
      var sse = new EventSource("/sse");
      sse.onmessage = function(e) {
        console.log(e.data);
      };
      sse.addEventListener(eventName, function() {
        var args = arguments;
        $rootScope.$apply(function () {
          callback.apply(sse, args);
        });
      });
    };


    return factory;
});
