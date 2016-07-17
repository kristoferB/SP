console.log('inside the shell driective file!!');
angular.module('app')
    .directive('shell', function() {
    return {
        scope: true,
        controller: 'ShellController',
        controllerAs: 'vm',
        templateUrl: 'app/layout/shell.html'
    };
});
