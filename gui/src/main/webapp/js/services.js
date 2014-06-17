'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
var serv = angular.module('myApp.services', []);

  serv.factory('events', function($http) {
    return {
      get: function(topic) {
        return $http.get('events/'+topic).then(function(result) {
            console.log(result);
            return result.data;
        });
      },
      post: function(topic, filter, pagination, sort) {
        if (typeof filter == "undefined") filter = {};
        if (typeof pagination == "undefined") pagination = {};
        if (typeof sort == "undefined") sort = {};
        var mess = {'filter': filter}
        return $http.post('events/'+topic, mess).then(function(res){
          return res.data
        });
      }
    }
   });

   serv.factory('charts', function($http) {
       return {
         getLeadTime: function(topic, filter, pagination, sort) {
           if (typeof filter == "undefined") filter = {};
           if (typeof pagination == "undefined") pagination = {'start': 0, 'no':10};
           if (typeof sort == "undefined") sort = {};
           var mess = {'filter': filter, 'pagination': pagination}
           return $http.post('chart/leadtime/'+topic, mess).then(function(res){
             return res.data
           });
         },
         getLeadTimeDistr: function(topic, filter, pagination, sort) {
            if (typeof filter == "undefined") filter = {};
            if (typeof pagination == "undefined") pagination = {'start': 0, 'no':10};
            if (typeof sort == "undefined") sort = {};
            var mess = {'filter': filter, 'pagination': pagination}
            return $http.post('chart/leadtimedistr/'+topic, mess).then(function(res){
              return res.data
            });
          },
          getProdPos: function(pid, filter, pagination, sort) {
              if (typeof filter == "undefined") filter = {};
              if (typeof pagination == "undefined") pagination = {'start': 0, 'no':10};
              if (typeof sort == "undefined") sort = {};
              var mess = {'pagination': pagination}
              return $http.post('chart/productpos/'+pid, mess).then(function(res){
                return res.data
              });
            },
        getPositions: function(topic, filter, pagination, sort) {
          if (typeof filter == "undefined") filter = {};
          if (typeof pagination == "undefined") pagination = {'start': 0, 'no':5000};
          if (typeof sort == "undefined") sort = {};
          var mess = {'filter': filter, 'pagination': pagination}
          return $http.post('chart/positions/'+topic, mess).then(function(res){
            return res.data
          });
        },
        getResourceState: function() {
                  return $http.get('chart/resourcestate').then(function(res){
                    return res.data
                  });
       },
       getResourceUtil: function() {
           return $http.get('chart/resourceutil').then(function(res){
             return res.data
           });
       },
       makeNewMachine: function() {
          return $http.get('chart/newmachine').then(function(res){
            console.log("hej")
            return res.data
          });
        }
      }});
