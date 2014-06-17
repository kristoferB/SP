'use strict';

/* Controllers */

var ctrl = angular.module('myApp.controllers', []);


  ctrl.controller('MainCtrl',  ['$scope', function($scope) {

  }]);

  ctrl.controller('MenuCtrl',  ['$scope', function($scope) {

  }]);


  ctrl.controller('TabCtrl',  ['$scope', 'events', function($scope, events) {

   $scope.tabs = [
      { title:'Dynamic Title 1', content:'Dynamic content 1' },
      { title:'Dynamic Title 2', content:'Dynamic content 2', disabled: true }
    ];

  }]);

  ctrl.controller('LisaDemoTab',  ['$scope', 'events', function($scope, events) {
    $scope.imageColsize = 'col-md-6';

    $scope.changeImgSize = function() {
        ($scope.imageColsize == 'col-md-6') ?
            $scope.imageColsize = 'col-md-12' :
            $scope.imageColsize = 'col-md-6'
    };

    $scope.getRawPosEvents = function(start, stop){
      var filter = {
             "kv": {},
             "startDate": start,
             "stopDate" : stop
           }
        events.post("positionraw", filter).then(function(result){
          console.log(result);
          $scope.rawPosEvents = result.res
          $scope.rawPosEventHits = result.no
          result
        })
    };

    $scope.getRawMachineEvents = function(start, stop){
      var filter = {
             "kv": {},
             "startDate": start,
             "stopDate" : stop
           }
        events.post("stateevents", filter).then(function(result){
          console.log(result);
          $scope.rawMachineEvents = result.res
          $scope.rawMachineEventHits = result.no
          result
        })

        };

    $scope.getFilledPosEvents = function(start, stop){
      var filter = {
             "kv": {},
             "startDate": start,
             "stopDate" : stop
           }
        events.post("positionfilled", filter).then(function(result){
          console.log(result);
          $scope.filledPosEvents = result.res
          $scope.filledPosEventHits = result.no
          result
        })

        };

    $scope.rawPosEvents = []
    $scope.rawPosEventHits = 0
    $scope.getRawPosEvents(new Date('2013-10-10'), new Date('2013-10-15'));

    $scope.filledPosEvents = []
    $scope.filledPosEventHits = 0
    $scope.getFilledPosEvents(new Date('2013-10-10'), new Date('2013-10-15'));

    $scope.rawMachineEvents = []
    $scope.rawMachineEventHits = 0
    $scope.getRawMachineEvents(new Date('2013-11-10'), new Date('2013-11-15'))

    $scope.selected = {};
    $scope.selectedF = {};
    $scope.selectedM = {};

    $scope.sort = {
        column: '',
        descending: false
    };
    $scope.sortM = {
         column: '',
         descending: false
    };


    $scope.changeSorting = function(column, table) {
                var sort = (table == 'm') ? $scope.sortM : $scope.sort;

                if (sort.column == column) {
                    sort.descending = !sort.descending;
                } else {
                    sort.column = column;
                    sort.descending = false;
                }
            };

  }]);

  ctrl.controller('RawEvents',  ['$scope', 'events', function($scope, events) {
           $scope.openstart = function($event) {
             $event.preventDefault();
             $event.stopPropagation();

             $scope.openedstart = true;
           };
            $scope.openstop = function($event) {
             $event.preventDefault();
             $event.stopPropagation();

             $scope.openedstop = true;
           };


           $scope.dateOptions = {
             formatYear: 'yy',
             startingDay: 1
           };

           $scope.dtstart = new Date('2013-10-10');
           $scope.dtstop = new Date('2013-10-12');
           $scope.format = 'yyyy-MM-dd';


        var temp = {
          "filter":{
            "kv": {},
            "startDate": "2013-10-08T19:41:01.000+02:00",
            "stopDate" : "2013-10-15T19:42:01.000+02:00"
          },
          "pagination": {"start": 0, "no": 10},
          "sort": {"attr": "", "des": false}
        }

  }]);





  ctrl.controller('ProductTab', ['$scope', 'events', 'charts',
                    function($scope, events, charts){
        $scope.chartObject = {};
        $scope.chartObjectDistr = {};
        $scope.chartObjectPos = {};


        $scope.getChartLeadTime = function(start, stop){
              var filter = {
                     "kv": {},
                     "startDate": start,
                     "stopDate" : stop
                   }
                charts.getLeadTime('productfold2',filter).then(function(result){
                  console.log(result);
                  $scope.chartObject.data = result
                  result
                })

                };

        $scope.getChartDistrLeadTime = function(start, stop){
                      var filter = {
                             "kv": {},
                             "startDate": start,
                             "stopDate" : stop
                           }
                       var pag = {
                            "start": 0,
                            "no": 5000
                          }
                        charts.getLeadTimeDistr('productfold2',filter, pag).then(function(result){
                          console.log(result);
                          $scope.chartObjectDistr.data = result
                          result
                        })

                      };

         $scope.getChartProdPos = function(pid){
                  var filter = {
                         "kv": {},
                         "startDate": new Date('2013-10-10'),
                         "stopDate" : new Date('2013-10-10')
                       }
                    charts.getProdPos(pid).then(function(result){
                      console.log(result);
                      $scope.chartObjectPos.data = result
                      result
                    })

                  };

        $scope.getChartLeadTime(new Date('2013-10-10'),new Date('2013-10-20'))
        $scope.getChartDistrLeadTime(new Date('2013-10-10'),new Date('2013-10-20'))
        $scope.chartObject.data = {}
        $scope.chartObjectDistr.data = {}
        $scope.chartObjectPos.data = {}


        // $routeParams.chartType == BarChart or PieChart or ColumnChart...
        $scope.chartObject.type = 'ColumnChart';
        $scope.chartObject.options = {
             'legend': 'none',
             'backgroundColor': '#4e5d6c',
             'colors': ['#df691a'],
             'chartArea': {'width': '80%', 'height': '80%'},
             'vAxis': {
                 titleTextStyle: {color: 'white'},
                 gridlines:{color: 'white'},
                 textStyle:{color: 'white'}},
             'hAxis': {
                 textStyle:{color: 'none'},
                 gridlines:{color: 'white'},
                 baselineColor:{color: 'white'},
                 color: 'none'}

         }
        $scope.chartObjectDistr.type = 'ColumnChart';
        $scope.chartObjectDistr.options = {
            'legend': 'none',
            'backgroundColor': '#4e5d6c',
            'colors': ['#df691a'],
            'chartArea': {'width': '80%', 'height': '80%'},
            'vAxis': {
                titleTextStyle: {color: 'white'},
                gridlines:{color: 'white'},
                textStyle:{color: 'white'}},
            'hAxis': {
                textStyle:{color: 'white'},
                gridlines:{color: 'white'},
                baselineColor:{color: 'white'},
                color: 'none'}

        }
        $scope.chartObjectPos.type = 'PieChart';
                $scope.chartObjectPos.options = {
                     'legend': 'none',
                     'backgroundColor': '#4e5d6c',
                     'height': 300,
                     'chartArea': {'width': '90%', 'height': '90%'},
                     'colors': ['#e0440e', '#e6693e', '#ec8f6e', '#f3b49f', '#f6c7b6'],
                     'vAxis': {
                         titleTextStyle: {color: 'white'},
                         gridlines:{color: 'white'},
                         textStyle:{color: 'white'}},
                     'hAxis': {
                         textStyle:{color: 'white'},
                         gridlines:{color: 'white'},
                         baselineColor:{color: 'white'},
                         color: 'none'}

                 }





        // table
            $scope.getProductFolds = function(start, stop){
              var filter = {
                     "kv": {},
                     "startDate": start,
                     "stopDate" : stop
                   }
                events.post("productfold2", filter).then(function(result){
                  console.log(result);
                  $scope.productFolds = result.res
                  $scope.productFoldHits = result.no
                  $scope.getChartLeadTime(start, stop)
                  $scope.getChartDistrLeadTime(start, stop)
                  result
                })
            };

            $scope.getProductFolds(new Date('2013-10-10'),new Date('2013-10-20'))
            $scope.productFolds = {}
            $scope.productFoldHits = 0

            $scope.selected = {};


  }]);

  ctrl.controller('PositionTab',  ['$scope', 'events', 'charts', function($scope, events, charts) {
    $scope.chartObject = {};

    $scope.getPositions = function(){
      var filter = {
             "kv": {},
             "startDate": new Date('2013-10-01'),
             "stopDate" : new Date('2013-10-31')
           }

        charts.getPositions('positionfold2',filter).then(function(result){
          console.log("positions:" +result);
          $scope.chartObject.data = result
          result
        })};

    $scope.getPositions()
    $scope.chartObject.data = {}

    $scope.chartObject.type = 'ColumnChart';
    $scope.chartObject.options = {
        'legend': 'none',
        'backgroundColor': '#4e5d6c',
        'colors': ['#df691a'],
        'height': 400,
        'chartArea': {'width': '90%', 'height': '90%'},
        'vAxis': {
            titleTextStyle: {color: 'white'},
            gridlines:{color: 'white'},
            textStyle:{color: 'white'}},
        'hAxis': {
            textStyle:{color: 'none'},
            gridlines:{color: 'white'},
            baselineColor:{color: 'white'},
            color: 'none'}

    }
  }]);

  ctrl.controller('ResourceTab',  ['$scope', 'charts', '$timeout', function($scope, charts, $timeout) {
      $scope.chartObject = {};
      $scope.resourceState = {}

      $scope.getResourceState = function(){
          var filter = {
                 "kv": {},
                 "startDate": new Date('2013-10-01'),
                 "stopDate" : new Date('2013-10-31')
               }

            charts.getResourceState().then(function(result){
              console.log("getResourceState");
              console.log(result);
              $scope.resourceState = result
              result
      })};

       $scope.getResourceUtil = function(){
           var filter = {
                  "kv": {},
                  "startDate": new Date('2013-10-01'),
                  "stopDate" : new Date('2013-10-31')
                }

             charts.getResourceUtil().then(function(result){
               console.log("resourceUtil");
               console.log(result);
               $scope.chartObject.data = result
               result
       })};

       $scope.tick = false

       $scope.getThem = function(){
            $scope.getResourceUtil()
            $scope.getResourceState()
            if ($scope.tick){
                $timeout(function() {
                        $scope.getThem()
                    }, 1000);
            }
       }

       $scope.getThem();

      $scope.newM = function(){
        charts.makeNewMachine()
        $scope.getThem()
      }



      $scope.chartObject.data = {}

      $scope.chartObject.type = 'ColumnChart';
      $scope.chartObject.options = {
          'legend': 'none',
          'isStacked': true,
          'backgroundColor': '#4e5d6c',
          'colors': ['#e0440e', '#e6693e', '#ec8f6e', '#f3b49f', '#f6c7b6'],
          'vAxis': {
              format: '#.##%',
              titleTextStyle: {color: 'white'},
              gridlines:{color: 'white'},
              textStyle:{color: 'white'}},
          'hAxis': {
              textStyle:{color: 'white'},
              gridlines:{color: 'white'},
              baselineColor:{color: 'white'},
              color: 'none'}

      }
    }]);
