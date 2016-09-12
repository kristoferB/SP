export class JsonNode {
    value: any;
    type: string;

    constructor(json: Object) {
	if(Array.isArray(json)){
    	    this.value = new Array<JsonNode>();
	    
    	    this.type = 'array'; //  'array' in not a type in javascript (it's just an object)
    	    var i = 0;
    	    for(let thing of (json as Array<Object>)) {
    		this.value.push(new JsonNode(thing));
    	    }
	} else if(typeof(json) == 'object') {
    	    this.value = new Array<[string, JsonNode]>();
    	    this.type = 'object';
    	    for(let myKey in json) {
    		this.value.push(
    		    [myKey, new JsonNode(json[myKey]) ]
    		);
    	    }
	} else { // current node is a leaf
    	    this.value = json;
    	    this.type = typeof(json); 
	}   
    }   
}
