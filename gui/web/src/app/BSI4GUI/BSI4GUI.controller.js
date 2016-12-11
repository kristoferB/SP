/**
 * Created by Martin on 2015-11-19.
 */
(function () {
	'use strict';

	angular
	.module('app.BSI4GUI')
	.controller('BSI4GUIController', BSI4GUIController);

	BSI4GUIController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
	/* @ngInject */
	function BSI4GUIController($scope, dashboardService, eventService,spServicesService) {

        // Initiates variables and functionj declarations.
            var vm = this;
            vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

            vm.result = [];
            vm.lock = 1;
            vm.buttonBG = "#ffffff"
            vm.activeColour = 0;
            vm.queuePlacement = 'Last';
            vm.Mode = 'Status';
            vm.Initialized = 0;
            var orderNr=0;
            var changeQueueOrder=0;
            var queuePlacement=0; // 0 =Last, 1=First, 2 = Next, 3 = Prev
            //functions that can be called from the html file
            vm.setActiveColour = setActiveColour;
            vm.saveNumber = saveNumber;
            vm.reset = reset;
            vm.sendOrder = sendOrder;
            vm.updateCubes = updateCubes;

            vm.queue = queue;
            vm.actuateQueue=actuateQueue;
            vm.status=status;
            vm.newOrder=newOrder;
            vm.currentOrder=currentOrder;
            vm.stopResume=stopResume;
		//Contains the colours of the cubes
		vm.ButtonColour = {
                // The status of the current position of coloured blocks
				Status: {     Left:    [[0, 0, 0, 0],
    								    [0, 0, 0, 0],
    								    [0, 0, 0, 0],
    								    [0, 0, 0, 0]],

    					       Middle: [[0, 0, 0, 0]],

				      	       Right:  [[0, 0, 0, 0],
				      	    	  	    [0, 0, 0, 0],
				    		   		    [0, 0, 0, 0],
				    		   		    [0, 0, 0, 0]]},
                // The block configuration for a new order
				NewOrder:	  {Left:   [[0, 0, 0, 0],
					    				[0, 0, 0, 0],
					    				[0, 0, 0, 0],
					    				[0, 0, 0, 0]],

					    	   Middle: [[0, 0, 0, 0]],

					    	   Right:  [[0, 0, 0, 0],
					    		   		[0, 0, 0, 0],
					    		   		[0, 0, 0, 0],
					    		   		[0, 0, 0, 0]]},
				// Used to see the orders in the build queue.
				CurrentOrder:{ Left:   [[0, 0, 0, 0],
									    [0, 0, 0, 0],
									    [0, 0, 0, 0],
									    [0, 0, 0, 0]],

							   Middle: [[0, 0, 0, 0]],

							   Right:  [[0, 0, 0, 0],
									    [0, 0, 0, 0],
									    [0, 0, 0, 0],
									    [0, 0, 0, 0]]}
				};

		vm.colourControlValue= {Status: 	 [0,0,0,0,0],
							   NewOrder: 	 [0,0,0,0,0],
							   CurrentOrder: [0,0,0,0,0]};

		activate();

        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        function onEvent(a) {
        	
        	
        		if (a.service =="BS"){
        			
        			
        		// when a status update is recived from the robots, i.e. they made a move, the status array of blocks will be updated	
        			 for (var i = 0; i < 4; i++) {
                vm.ButtonColour.Status.Middle[0][i]= a.attributes.middle[i];
             
                for (var j = 0; j < 4; j++) {
                	
                    vm.ButtonColour.Status.Left[i][j]= a.attributes.left[i*4+j];
                    vm.ButtonColour.Status.Right[i][j]= a.attributes.right[i*4+j];
                }
            }	
            if(vm.Mode=='Status')
        			updateCubes('Status')
        	//-------------------------------------------------------------------------------------------------------------------------		
        			
        			
        			
        			
					//var keys = Object.keys(a.attributes);
					//alert(keys);
					//usingLeftRobot,usingMiddle,isPicking,position,color
					var op ="";	
					/*				
					if(a.attributes.moves[0].usingLeftRobot){
						op += "The left robot ";}
					else{
						op += "The right robot ";
						}
					*/
					if (a.attributes.moves[0].isPicking) 
					   op += "Pick ";
					else 
					   op += "Place ";
					switch(a.attributes.moves[0].color) {
					case 1:
					op += "blue ";
					 break;
					case 2:
					op += "green ";
					break;
					case 3:
					op += "red ";
					break;
					case 4:
					op += "yellow ";
					}	   
					op+= ",pos: "
					op+= String((a.attributes.moves[0].position))
					op+= " "
					if (a.attributes.moves[0].usingMiddle) 
						op += ", middle ";
					else if (a.attributes.moves[0].usingLeftRobot){
						op += ",left ";
					}
					else 
						op+= ", right "
						
						
					   var OperationList = document.createElement("li");
  						var t = document.createTextNode(op);		
  						OperationList.appendChild(t);
  						
  						var buttonNr=computePositionRowColFromSpEvent(a.attributes.moves[0].usingMiddle,a.attributes.moves[0].usingLeftRobot,a.attributes.moves[0].position);
						OperationList.setAttribute("id", buttonNr);  						
  						//OperationList.setAttribute("data-bntNr", buttonNr);
  						if(a.attributes.moves[0].usingLeftRobot)
    						document.getElementById("SoplistLeft").appendChild(OperationList);
						else						
							document.getElementById("SoplistRight").appendChild(OperationList);
        		}
            //console.log("It has to be done");
            //reset();
        }

function computePositionRowColFromSpEvent(usingMiddle,usingLeftRobot,Position) {
	           var buttonNr ="button";
					if (usingMiddle)
						buttonNr +="3";
					else if (usingLeftRobot)
						buttonNr += "1";
					else 
						buttonNr+="2";
					
				   if (Position<4)
				      buttonNr += "1" +String(Position+1);
				   else if (Position<8)
				   	buttonNr += "2" +String(Position-3);
				   else if (Position<12)
				      buttonNr += "3"+String(Position-7);
				   else if (Position<16)
				      buttonNr += "4"+String(Position-11); 
				      
				   return buttonNr;          	
}
		function setActiveColour(int) {
			if(vm.Initialized ==0 && vm.Mode=='Status' || vm.Mode=='NewOrder' && vm.Initialized ==1){
				vm.activeColour = int;
				switch (vm.activeColour) {
				case 1://BLUE = 1
					vm.buttonBG = "#0066ff";
					break;
				case 2://GREEN = 2
					vm.buttonBG = "#5cd65c";
					break;
				case 3://RED = 3
					vm.buttonBG = "#ff3333";
					break;
				case 4://YELLOW = 4
					vm.buttonBG = "#ffff66";
					break;
				default:
					vm.buttonBG= "#FFFFFF";
				}
			}
		}

		function status(){
            vm.Mode = 'Status';
            updateStatusButtonAndText();
            if (vm.Initialized ==0){
                vm.Initialized = 1;
                vm.lock=1;
                sendInitial();
                document.getElementById('StatusButtonText').innerHTML='Status';
                document.getElementById('Status').dataset.info='Show the current block configuration';
            }
            updateCubes('Status');
        }
        function updateStatusButtonAndText(){
        	if( ! (($("ul#SoplistLeft").has("li").length === 0) && ($("ul#SoplistRight").has("li").length === 0)))
        	   document.getElementById('Sop').style.display='Block';
        	   
          document.getElementById('NewOrder').style.backgroundColor = "#ffffff";
          document.getElementById('CurrentOrder').style.backgroundColor = "#ffffff";
          document.getElementById("buttonQueueActuate").style.visibility="hidden";
          document.getElementById('Status').style.backgroundColor= "#5cd65c";
          document.getElementById('NewOrder').value = 'NewOrder';
          document.getElementById('OrderButtonText').innerHTML = 'New Order';
          document.getElementById('ResetButtonText').innerHTML='Reset all';
          document.getElementById('buttonReset').dataset.info='Reset the robots, the build queue and initial positions';
          document.getElementById('OrderNrButtonText').innerHTML='Current Order';
          orderNr=0;
          document.getElementById('TextInfo').innerHTML='Current status of the blocks';
        }

        function newOrder(){
         if (vm.Initialized == 1){
                vm.Mode = 'NewOrder';
                if (document.getElementById(vm.Mode).value==vm.Mode){
                    updateOrderButtonAndText();
                    copyButtonColourValues('NewOrder','Status');
                    updateCubes(vm.Mode);
                    ResetControlColorValue(vm.Mode);
                    updateControlColour(vm.Mode);
                    vm.lock=0;
                }
                else if(document.getElementById(vm.Mode).value=='PlaceOrder'){
                                          
                    if (testIsValidBlockConfiguration()){   
                        updateOrderPlacedButtonAndText();
                        updateCubes(vm.Mode);
                        sendOrder();
                        copyButtonColourValues('CurrentOrder','NewOrder');
                        vm.lock = 1;
                     }
                     else {alert("At least one block must be placed in a new location and you have to specify all of the blocks locations.")}
                   
                }
            }
        }
        function updateOrderButtonAndText(){
            document.getElementById('CurrentOrder').style.backgroundColor= "#ffffff";
            document.getElementById('Status').style.backgroundColor= "#ffffff";
            document.getElementById('NewOrder').style.backgroundColor= "#5cd65c";
            document.getElementById('ResetButtonText').innerHTML='Clear blocks';
            document.getElementById('buttonReset').dataset.info='Clear the plates of blocks';
            document.getElementById('OrderNrButtonText').innerHTML='Current Order';
            document.getElementById('Sop').style.display='none';
            orderNr=0;
            changeQueueOrder = 0;
            document.getElementById("buttonQueueActuate").style.visibility="hidden";
            document.getElementById('NewOrder').value='PlaceOrder';
            document.getElementById('OrderButtonText').innerHTML='Place Order';
            document.getElementById('TextInfo').innerHTML='Reconfigure the blocks and place order';
        }
        function updateOrderPlacedButtonAndText(){
        	$('#SoplistLeft').empty();
		   $('#SoplistRight').empty();
        	document.getElementById('Sop').style.display='Block';
            document.getElementById('NewOrder').style.backgroundColor= "#ffffff";
            document.getElementById('NewOrder').value='NewOrder';
            document.getElementById('OrderButtonText').innerHTML='New Order';
            document.getElementById('TextInfo').innerHTML='Order placed';
        }

        function currentOrder(){
            if (vm.Initialized == 1){
                vm.Mode = 'CurrentOrder';
                //orderNr++;
                updateCurrentOrderButtonAndText(vm.Mode);
                updateCubes(vm.Mode);
            }
        }
        function updateCurrentOrderButtonAndText(){
        	   document.getElementById('Sop').style.display='Block';
            document.getElementById("buttonQueueActuate").style.visibility="visible";
            document.getElementById('Status').style.backgroundColor= "#ffffff";
            document.getElementById('NewOrder').style.backgroundColor= "#ffffff";
            document.getElementById('CurrentOrder').style.backgroundColor= "#5cd65c";
            if(orderNr>1) {
                document.getElementById('OrderNrButtonText').innerHTML = 'Order nr:' + orderNr;
                document.getElementById('TextInfo').innerHTML = 'This is order nr: ' + orderNr + ' in the queue';
            }
            else {
                document.getElementById('OrderNrButtonText').innerHTML = 'Current Order';
                document.getElementById('TextInfo').innerHTML = 'This is the current order under construction';
            }
            document.getElementById('OrderButtonText').innerHTML='New Order';
            document.getElementById('NewOrder').value='NewOrder';
            document.getElementById('ResetButtonText').innerHTML='Terminate Order';
            document.getElementById('buttonReset').dataset.info='Remove order from the queue';

        }

        function ResetControlColorValue(Mode){
            vm.colourControlValue[Mode][1]=0;
            vm.colourControlValue[Mode][2]=0;
            vm.colourControlValue[Mode][3]=0;
            vm.colourControlValue[Mode][4]=0;
        }
        function updateControlColour(Mode){
            document.getElementById(1).value=vm.colourControlValue[Mode][1];
            document.getElementById(2).value=vm.colourControlValue[Mode][2];
            document.getElementById(3).value=vm.colourControlValue[Mode][3];
            document.getElementById(4).value=vm.colourControlValue[Mode][4];
        }

		// Sends a new order to the BSservice
		function sendOrder() {
			var mess = {"data": {"command": "order",
				"Left": vm.ButtonColour.NewOrder.Left,
				"Middle": vm.ButtonColour.NewOrder.Middle,
				"Right": vm.ButtonColour.NewOrder.Right}};

			spServicesService.callService(spServicesService.getService("BSservice"),
					mess,
					function (resp) {
				if (_.has(resp, 'attributes.result')) {
					console.log("Hej" + vm.result);
				}
			}
			)
		}
		
				function sendInitial() {
			var mess = {"data": {"command": "setup",
				"Left": vm.ButtonColour.Status.Left,
				"Middle": vm.ButtonColour.Status.Middle,
				"Right": vm.ButtonColour.Status.Right}};

			spServicesService.callService(spServicesService.getService("BSservice"),
					mess,
					function (resp) {
				if (_.has(resp, 'attributes.result')) {
					console.log("Hej" + vm.result);
				}
			}
			)
		}

        // Change the colour of a button.
		function setColor(btn,Mode,location, row, column, setOnly, reset) {

			var property = document.getElementById(btn);
			if (reset) {
					property.style.backgroundColor = "#FFFFFF";
					vm.ButtonColour[Mode][location][row][column] = 0;
			}
			else{
				if(vm.Initialized ==0 && Mode=='Status' || Mode=='NewOrder' && vm.Initialized ==1 && vm.lock==0){
					if (!setOnly && vm.activeColour>0) {

                        // If the colour is already the same as the selected one
						if(vm.ButtonColour[Mode][location][row][column] == vm.activeColour){
							vm.ButtonColour[Mode][location][row][column] = 0;
							if(Mode=='Status'){
								vm.colourControlValue[Mode][vm.activeColour] --;
							}
							else if(Mode=='NewOrder'){
								vm.colourControlValue[Mode][vm.activeColour] ++;
							}
						}
						// Otherwise
						else{
                            // If there is another coloured block on that position
							if(vm.ButtonColour[Mode][location][row][column] != 0){
								if(Mode=='Status'){
									vm.colourControlValue[Mode][vm.ButtonColour[Mode][location][row][column]] --;
								}
								else if(Mode=='NewOrder' && vm.colourControlValue[Mode][vm.activeColour]>0){
									vm.colourControlValue[Mode][vm.ButtonColour[Mode][location][row][column]] ++;
								}
							}

							//Colour the block and update the colour control counter value
							if(Mode=='Status'){
								vm.colourControlValue[Mode][vm.activeColour] ++;
								vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
							}
							else if(Mode=='NewOrder' && vm.colourControlValue[Mode][vm.activeColour]>0){
								vm.colourControlValue[Mode][vm.activeColour] --;
								vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
							}
						}
					}
				}
				colourTheBlock(Mode,location,row,column,property);
			}
            updateControlColour(Mode);
		}

		function colourTheBlock(Mode,location,row,column,property){
            switch (vm.ButtonColour[Mode][location][row][column]) {
                case 1://BLUE = 1
                    property.style.backgroundColor = "#0066ff";
                    break;
                case 2://GREEN = 2
                    property.style.backgroundColor = "#5cd65c";
                    break;
                case 3://RED = 3
                    property.style.backgroundColor = "#ff3333";
                    break;
                case 4://YELLOW = 4
                    property.style.backgroundColor = "#ffff66";
                    break;
                default: //white by default
                    property.style.backgroundColor = "#FFFFFF";
            }
        }

		function saveNumber(btn,location,row, column) {
			setColor(btn,vm.Mode,location, row, column, 0, 0);
			//tryTheTower(0);
		}

		 // Updates the colour of the button, without changing it!
		function updateCubes(Mode) {
			for (var i = 0; i < 4; i++) {
				setColor('button' + eval(i + 311),Mode,'Middle', 0, i, 1, 0);
				for (var j = 0; j < 4; j++) {
					setColor('button' + eval(i * 10 + j + 111),Mode,'Left', i, j, 1, 0);
					setColor('button' + eval(i * 10 + j + 211),Mode,'Right', i, j, 1, 0);
				}
			}
		}

		function reset() {

            if(vm.Mode=='Status'){ // Resets everything
                vm.lock=0;
                vm.Initialized = 0;
                document.getElementById('StatusButtonText').innerHTML='Confirm';
                document.getElementById('Status').dataset.info='Confirm the initial positions of the blocks';
                document.getElementById('TextInfo').innerHTML='Initialize positions of the blocks and confirm';
                ResetControlColorValue(vm.Mode);
                document.getElementById('ProggressUpdate').innerHTML='No orders under construction';
                document.getElementById('Sop').style.display='none';
					 $('#SoplistLeft').empty();
					 $('#SoplistRight').empty();
                resetAllBlocks();
            }
            else if(vm.Mode=='NewOrder' && vm.lock==0){ // Only resets the colours of the NewOrder
                vm.colourControlValue[vm.Mode][1]=vm.colourControlValue['Status'][1];
                vm.colourControlValue[vm.Mode][2]=vm.colourControlValue['Status'][2];
                vm.colourControlValue[vm.Mode][3]=vm.colourControlValue['Status'][3];
                vm.colourControlValue[vm.Mode][4]=vm.colourControlValue['Status'][4];
                resetAllBlocks();
            }
		}

        // Set the colour and value of the blocks to 0 /white
		function resetAllBlocks(){
            for (var i = 0; i < 4; i++) {
                setColor('button' + eval(i + 311), vm.Mode, 'Middle', 0, i, 0, 1);
                for (var j = 0; j < 4; j++) {
                    setColor('button' + eval(i * 10 + j + 111), vm.Mode, 'Left', i, j, 0, 1);
                    setColor('button' + eval(i * 10 + j + 211), vm.Mode, 'Right', i, j, 0, 1);
                }
            }
        }
		
		function copyButtonColourValues (Mode1, Mode2){
			for (var i = 0; i < 4; i++) {
					vm.ButtonColour[Mode1].Middle[0][i]=vm.ButtonColour[Mode2].Middle[0][i];
                for (var j = 0; j < 4; j++) {
                	vm.ButtonColour[Mode1].Left[i][j]=vm.ButtonColour[Mode2].Left[i][j];
                	vm.ButtonColour[Mode1].Right[i][j]=vm.ButtonColour[Mode2].Right[i][j];
                }
            }
		}
        // Pauses or resumes the robots current operation
		function stopResume(){

			var mess = {"data": {"command": "hej",
				"Left": vm.ButtonColour.NewOrder.Left,
				"Middle": vm.ButtonColour.NewOrder.Middle,
				"Right": vm.ButtonColour.NewOrder.Right}};

			spServicesService.callService(spServicesService.getService("BSservice"),
					mess,
					function (resp) {
				if (_.has(resp, 'attributes.result')) {
					console.log("Hej" + vm.result);
				}
			}
			)
			
			
			
			
            if(vm.Initialized == 1) {
                if (document.getElementById('StopResumeButtonText').innerHTML == 'Stop') {
                    document.getElementById('StopResumeButtonText').innerHTML = 'Resume';
                    document.getElementById('ProggressUpdate').innerHTML = 'All orders have been put on hold';
                }
                else {
                    document.getElementById('StopResumeButtonText').innerHTML = 'Stop';
                    document.getElementById('ProggressUpdate').innerHTML = 'Resuming operations';
                }
            }
		}

		//Determines where the order will be placed in the queue
		function queue(){
            if(vm.Mode=='NewOrder' || vm.Mode=='CurrentOrder') {
                switch (document.getElementById('buttonQueue').value) {
                    case "1":
                        document.getElementById('buttonQueue').value = '2';
                        document.getElementById('buttonQueueText').innerHTML = 'Place first in queue';
                        break;
                    case "2":
                        document.getElementById('buttonQueue').value = '3';
                        document.getElementById('buttonQueueText').innerHTML = 'Place next in queue';
                        break;
                    case "3":
                        document.getElementById('buttonQueue').value = '4';
                        document.getElementById('buttonQueueText').innerHTML = 'Place prev in queue';
                        if(vm.Mode=='CurrentOrder')
                        break;
                    case "4":
                        document.getElementById('buttonQueue').value = '1';
                        document.getElementById('buttonQueueText').innerHTML = 'Place last in queue';
                }
            }
		}

		function actuateQueue() {
            if (changeQueueOrder==1)
                changeQueueOrder = 0;
            else
                changeQueueOrder = 1;
        }
        
         function testIsValidBlockConfiguration(){
            var newOrderIsSameAsStatus= _.isEqual(vm.ButtonColour.Status.Right, vm.ButtonColour.NewOrder.Right) &&
                _.isEqual(vm.ButtonColour.Status.Left, vm.ButtonColour.NewOrder.Left) &&
                _.isEqual(vm.ButtonColour.Status.Middle, vm.ButtonColour.NewOrder.Middle);
            var allBlocksHaveBeenPlaced=_.isEqual(vm.colourControlValue.NewOrder,[0,0,0,0,0]);
            return !newOrderIsSameAsStatus && allBlocksHaveBeenPlaced;

        }

	}
})();