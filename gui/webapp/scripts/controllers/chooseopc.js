'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ChooseOPCCtrl
 * @description
 * # ChooseOPCCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('ChooseOPCCtrl', function ($scope, $rootScope, $modalInstance, $modal, spTalker, notificationService, postToRuntime) {

  $scope.spTalker = spTalker;

  $scope.chooseOPC = function(opc) {
    var data = {
      action: 'chooseOPC',
        opcSpecID: opc.id
    };
    function success() {
      $modalInstance.close();
    }
    postToRuntime(data, success);
  };

  $scope.editOPC = function(opc) {
    $modal.open({
      templateUrl: 'views/editopc.html',
      controller: 'EditOPCCtrl',
      resolve: {
        opc: function () {
          return opc;
        }
      }
    });
  };

  $scope.newOPC = function() {
    var opc = {
      name: "New OPC",
      isa: "SPSpec",
      attributes: {
        type: "OPCSpec",
        ip: "192.168.0.1",
        port: 9000,
        tagPrefix: "",
        idsToTags: {}
      }
    };
    function successHandler(opc) {
      $rootScope.$broadcast('itemsQueried');
      $scope.editOPC(opc);
    }
    spTalker.createItem("SPSpec", successHandler, opc, false, false, false)
  };


  $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };

});
