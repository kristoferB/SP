/* jshint -W117, -W030 */
describe('ModelsController', function() {
    var controller;
    var models = mockData.getMockModels();

    beforeEach(function() {
        bard.appModule('app.models');
        bard.inject('eventService', '$httpBackend', 'restService', '$controller', '$log', '$rootScope', '$q', '$state');
        $state.current = {title: 'Models'};
        /* global EventSource */
        eventService.eventSource = new EventSource('/');
        bard.inject('modelService');
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('ModelsController', function() {
        it('should log an error if the GET fails', function() {
            $httpBackend.expectGET('/api/models').respond(500);
            controller = $controller('ModelsController');
            $rootScope.$apply();
            $httpBackend.flush();
            expect($log.error.logs).to.match(/Query for/);
        });

        describe('after activate', function() {
            beforeEach(function() {
                $httpBackend.expectGET('/api/models').respond(200, models);
                controller = $controller('ModelsController');
                $rootScope.$apply();
                $httpBackend.flush();
            });

            it('should be created successfully', function () {
                expect(controller).to.be.defined;
            });

            it('should have title of Models', function() {
                expect(controller.title).to.equal('Models');
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });

            it('should have at least 1 model', function () {
                expect(controller.models).to.have.length.above(0);
            });

            it('should have model count of 2', function () {
                expect(controller.models).to.have.length(2);
            });

            describe('on a model creation event', function() {
                beforeEach(function() {
                    var modelCreationEvent = mockData.getMockModelCreationEvent();
                    eventService.eventSource.dispatchEvent(modelCreationEvent);
                });

                it('should result in a model count of 3', function () {
                    expect(controller.models).to.have.length(3);
                });

                it('should log "Added"', function() {
                    expect($log.info.logs).to.match(/Added/);
                });
            });

            describe('should after a model deletion event', function() {
                beforeEach(function() {
                    var modelDeletionEvent = mockData.getMockModelDeletionEvent();
                    eventService.eventSource.dispatchEvent(modelDeletionEvent);
                });

                it('have model count of 1', function () {
                    expect(controller.models).to.have.length(1);
                });

                it('have a specific model left', function () {
                    expect(controller.models[0].id).to.equal('5ccdc932-438d-4c45-b947-6dd208e31e31');
                });

                it('have logged "Removed"', function() {
                    expect($log.info.logs).to.match(/Removed/);
                });
            });

            describe('should after a model update event', function() {
                beforeEach(function() {
                    var modelUpdateEvent = mockData.getMockModelUpdateEvent();
                    eventService.eventSource.dispatchEvent(modelUpdateEvent);
                    this.oneModel = modelService.getModel('5ccdc932-438d-4c45-b947-6dd208e31e31');
                });

                it('still have model count of 2', function () {
                    expect(controller.models).to.have.length(2);
                });

                it('have one of the model names updated', function () {
                    expect(this.oneModel.name).to.equal('An updated name');
                });

                it('have one of the model versions bumped to 2', function () {
                    expect(this.oneModel.version).to.equal(2);
                });

                it('have one of the model time stamps updated', function () {
                    expect(this.oneModel.attributes.time).to.equal('2015-07-31T15:23:07.276+0200');
                });

                it('have logged "Updated"', function() {
                    expect($log.info.logs).to.match(/Updated/);
                });
            });

            describe('should after a model creation click', function() {
                var data = {'name': 'A new model'};

                it('POST a chosen model name to correct URL', function () {
                    $httpBackend.expectPOST('/api/models', data).respond(200);
                    modelService.createModel('A new model');
                    $httpBackend.flush();
                });

                it('should log an error if the POST fails', function () {
                    $httpBackend.expectPOST('/api/models', data).respond(500);
                    modelService.createModel('A new model');
                    $httpBackend.flush();
                    expect($log.error.logs).to.match(/Post of data to/);
                });
            });

            describe('after a model name update', function() {
                var aModel, postData, aNewName;

                beforeEach(function() {
                    aModel = modelService.getModel('5ccdc932-438d-4c45-b947-6dd208e31e31');
                    data = {};
                    aNewName = 'A fancier name';
                    angular.extend(data, aModel);
                    data.name = aNewName;
                });

                it('should POST the new model name to the correct URL', function () {
                    $httpBackend.expectPOST('/api/models/5ccdc932-438d-4c45-b947-6dd208e31e31', postData).respond(200);
                    modelService.updateName(aModel, aNewName);
                    $httpBackend.flush();
                });

                it('should log an error if the POST fails', function () {
                    $httpBackend.expectPOST('/api/models/5ccdc932-438d-4c45-b947-6dd208e31e31', postData).respond(500);
                    modelService.updateName(aModel, aNewName);
                    $httpBackend.flush();
                    expect($log.error.logs).to.have.length(1);
                });

            });

            describe('should after a model deletion click', function() {
                it('send a DELETE request to the correct URL', function () {
                    $httpBackend.expectDELETE('/api/models/5ccdc932-438d-4c45-b947-6dd208e31e31').respond(200);
                    modelService.deleteModel('5ccdc932-438d-4c45-b947-6dd208e31e31');
                    $httpBackend.flush();
                });

                it('should log a deletion error if the DELETE request fails', function () {
                    $httpBackend.expectDELETE('/api/models/5ccdc932-438d-4c45-b947-6dd208e31e31').respond(500);
                    modelService.deleteModel('5ccdc932-438d-4c45-b947-6dd208e31e31');
                    $httpBackend.flush();
                    expect($log.error.logs).to.match(/Deletion of/);
                });
            });
        });
    });
});
