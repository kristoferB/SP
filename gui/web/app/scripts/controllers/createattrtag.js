/**
 * Created by daniel on 2014-08-20.
 */

angular.module('spGuiApp')
  .controller('CreateAttrTagCtrl', function ($scope, $modalInstance, attrTagObj) {
    $scope.attrTagTypes = [
      {value: '', label: 'String'},
      {value: false, label: 'Boolean'},
      {value: 0, label: 'Number'},
      {value: {itemid: ''}, label: 'Item'},
      {value: [], label: 'List'},
      {value: {}, label: 'Map'},
      {value: new Date(), label: 'Date'}
    ];

    $scope.forms = {
      tag: '',
      type: $scope.attrTagTypes[0]
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.save = function() {
      attrTagObj[$scope.forms.tag] = $scope.forms.type.value;
      $scope.close();
    };


  });
