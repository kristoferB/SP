'use strict';

angular.module('spGuiApp')
    .directive('processsimulate', function (spTalker, tabSvc, notificationService) {
	return {
	    templateUrl: 'views/processsimulateview.html',
	    restrict: 'E',
	    link: function postLink(scope) {
		scope.ps_import = function() {
		    var txid = "1,143"
		    var res = spTalker.importFromPS(txid);
		    res.success(function (data) {
			notificationService.success('Import ok.');
		    });
		    res.error(function(data) {
			notificationService.error('Import failed');
		    });
		}
		scope.ps_createOP = function() {
		    var opname = "New operation from SP";
		    var res = spTalker.createOp(opname);
		    res.success(function (data) {
			notificationService.success('Operation created.');
		    });
		    res.error(function(data) {
			notificationService.error('Operaion creation failed');
		    });
		}
	    }
	};
    });
