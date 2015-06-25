'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:createDropdown
 * @description
 * # createDropdown
 */
angular.module('spGuiApp')
  .directive('createDropdown', function (itemListSvc) {
    return {
      restrict: 'A',
      link: function postLink(scope, element) {

        $(element)
          .on( 'click', function( e ) {
            e.stopPropagation();
            $(this).contextmenu( 'show', e );
          } )
          .contextmenu({
          target:'#create-context-menu',
          onItem: function(context,e) {
            var key = e.target.getAttribute('id');
            if(key === 'StateVariable') {
              itemListSvc.addStateVar(scope.item, scope.itemListScope);
            } else {
              itemListSvc.createItem(key, scope.item, scope.itemListScope);
            }
          }
        });

      }
    };
  });
