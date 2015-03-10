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
  var sse = new EventSource("/eventsource"),
    factory = {};

    factory.addSSEListener = function(eventName, callback) {
      console.log("Adding an SSE listener");
      sse.addEventListener(eventName, function() {
        var args = arguments;
        $rootScope.$apply(function () {
          callback.apply(sse, args);
        });
      });
    };

    return factory;
});
