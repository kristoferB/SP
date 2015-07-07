/* jshint -W079 */
var mockData = (function() {
    return {
        getMockModels: getMockModels,
        getMockStates: getMockStates
    };

    function getMockStates() {
        return [
            {
                state: 'dashboard',
                config: {
                    url: '/',
                    templateUrl: 'app/dashboard/dashboard.html',
                    title: 'dashboard',
                    settings: {
                        nav: 1,
                        content: '<i class="fa fa-dashboard"></i> Dashboard'
                    }
                }
            }
        ];
    }

    function getMockModels() {
        return [
            {id:'5689ce13-2e2d-4451-8f8f-f05973f78dfb', name:'A first model', version:46,
                attributes:{time:'2015-07-07T18:22:07.125+0200'}},
            {id:'5ccdc932-438d-4c45-b947-6dd208e31e31', name:'A second model', version:72,
                attributes:{time:'2015-07-07T15:10:16.970+0200'}}
        ];
    }
})();
