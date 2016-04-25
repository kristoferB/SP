/* jshint -W117, -W030 */
describe('rasmusController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.rasmus');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'rasmus'};
    });

    beforeEach(function () {
        controller = $controller('rasmusController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('rasmus controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have empty log', function() {
                expect(controller.eventLog).to.equal([]);
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });
        });
    });
});
