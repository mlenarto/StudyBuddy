Parse.Cloud.afterSave("HelpRequest", function(request) {
	
	var query = new Parse.Query(Parse.Installation);
	var string = request.object.get("course").title;
	query.equalTo('channels', 'CS18000');

	Parse.Push.send({
		where: query,
		data: { alert: "Someone in CS18000 needs your help!" }
	},
	{
		success: function() {
		//Push was successful
		},
    		error: function(error) {
      		console.error("Got an error " + error.code + " : " + error.message);
    		}
  	});
});
