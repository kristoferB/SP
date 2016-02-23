/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.Tobbe')
      .controller('TobbeController', TobbeController);

    TobbeController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function TobbeController($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what


        vm.a = 0;
        vm.b = 0;
        vm.result = 0;

        vm.bigR = 255;
        vm.$var = 0;
        vm.resultColor = '#00FF00';
        vm.getProperColor = getProperColor;
        vm.saveNumber = saveNumber;
        vm.reset = reset;

        vm.calc = calc;

        activate();


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function calc(sign){
            //spServicesService.callService();
            var mess = {"data": {"a":vm.a, "b":vm.b, "sign":sign}};

            spServicesService.callService(spServicesService.getService("TobbeG"),
                mess,
                function(resp){
                    if (_.has(resp, 'attributes.result')){
                        vm.result = resp.attributes.result;
                    }
                }
            )
        }
/*
        function getProperColor($number)
        {
            var mess = {"color": {"number":number}};

            spServicesService.callService(spServicesService.getService("TobbeG"),
                mess,
                function(resp){
                    if (_.has(resp, 'attributes.result')){
                        vm.resultColor = resp.attributes.result;
                    }
                }
            )
        }

 */
        vm.button41 = {Value: 0};vm.button42 = {Value: 0};vm.button43 = {Value: 0};vm.button44 = {Value: 0};
        vm.button31 = {Value: 0};vm.button32 = {Value: 0};vm.button33 = {Value: 0};vm.button34 = {Value: 0};
        vm.button21 = {Value: 0};vm.button22 = {Value: 0};vm.button23 = {Value: 0};vm.button24 = {Value: 0};
        vm.button11 = {Value: 0};vm.button12 = {Value: 0};vm.button13 = {Value: 0};vm.button14 = {Value: 0};

        vm.vmarr = [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0]
        ];
        /**
         * This function resets the colour of every button.
         * It does this by passing "1" as an argument to
         * the function setColour in the .html file.
         */
        function reset(){
            //Row 4
            setColor('button41', vm.button41, 1);
            setColor('button42', vm.button42, 1);
            setColor('button43', vm.button43, 1);
            setColor('button44', vm.button44, 1);
            //Row 3
            setColor('button31', vm.button31, 1);
            setColor('button32', vm.button32, 1);
            setColor('button33', vm.button33, 1);
            setColor('button34', vm.button34, 1);
            //Row 2
            setColor('button21', vm.button21, 1);
            setColor('button22', vm.button22, 1);
            setColor('button23', vm.button23, 1);
            setColor('button24', vm.button24, 1);
            //Row 1
            setColor('button11', vm.button11, 1);
            setColor('button12', vm.button12, 1);
            setColor('button13', vm.button13, 1);
            setColor('button14', vm.button14, 1);
        }

        /**
         * Denna funktionen kör setColor functionen.
         * Om det inte behövs köras något mer kan den
         * tas bort och spridas ut bland knapparna.
         * @param number
         */
        function saveNumber(number){

            setColor('button' + number, eval('vm.button' + number), 0);

            /*switch(number) {
                case 41:
                    setColor('button41', vm.button41, 0);
                    break;
                case 42:
                    setColor('button42', vm.button42, 0);
                    break;
                case 43:
                    setColor('button43', vm.button43, 0);
                    break;
                case 44:
                    setColor('button44', vm.button44, 0);
                    break;
                case 31:
                    setColor('button31', vm.button31, 0);
                    break;
                case 32:
                    setColor('button32', vm.button32, 0);
                    break;
                case 33:
                    setColor('button33', vm.button33, 0);
                    break;
                case 34:
                    setColor('button34', vm.button34, 0);
                    break;
                case 21:
                    setColor('button21', vm.button21, 0);
                    break;
                case 22:
                    setColor('button22', vm.button22, 0);
                    break;
                case 23:
                    setColor('button23', vm.button23, 0);
                    break;
                case 24:
                    setColor('button24', vm.button24, 0);
                    break;
                case 11:
                    setColor('button11', vm.button11, 0);
                    break;
                case 12:
                    setColor('button12', vm.button12, 0);
                    break;
                case 13:
                    setColor('button13', vm.button13, 0);
                    break;
                case 14:
                    setColor('button14', vm.button14, 0);
                    break;
            }
            */

        }

        function getProperColor()
        {
            if ($var > 0 && $var <= 5)
                vm.resultColor = '#00FF00';
            else if ($var >= 6 && $var <= 10)
                vm.resultColor = '#FF8000';
            else if ($var >= 11)
                vm.resultColor = '#FF0000';
        }


    }
})();
