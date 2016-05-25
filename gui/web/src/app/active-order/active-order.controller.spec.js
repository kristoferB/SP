/* jshint -W117, -W030 */
describe('activeOrderController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.activeOrder');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'activeOrder'};
    });

    beforeEach(function () {
        controller = $controller('activeOrderController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('activeOrder controller', function() {
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
