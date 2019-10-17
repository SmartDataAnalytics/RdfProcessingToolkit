package org.aksw.sparql_integrate.cli;

import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Quad;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SPARQLResultVisitorSelectJsonOutput
		implements SPARQLResultSink
	{
		protected JsonArray arr;
		protected int maxDepth = 3;
		protected Gson gson;
		
		public SPARQLResultVisitorSelectJsonOutput() {
			this(null, null, null);
		}

		public SPARQLResultVisitorSelectJsonOutput(Gson gson) {
			this(null, null, gson);
		}
		
		public SPARQLResultVisitorSelectJsonOutput(JsonArray arr, Integer maxDepth, Gson gson) {
			super();
			this.arr = arr != null ? arr : new JsonArray();
			this.maxDepth = maxDepth != null ? maxDepth : 3;
			this.gson = gson != null ? gson : new Gson();
		}

		@Override
		public void onResultSet(ResultSet value) {
			JsonElement json = JsonUtils.toJson(value, maxDepth);
			onJson(json);
		}

		@Override
		public void onJson(JsonElement value) {
			//String str = gson.toJson(value);
			arr.add(value);
		}

		@Override
		public void onQuad(Quad value) {
			System.err.println(value);
		}

		@Override
		public void close() throws Exception {
			// Return the last item of the json array
			JsonElement tmp = arr.size() == 0
					? new JsonObject()
					: arr.get(arr.size() - 1);
//			JsonElement tmp = arr.size() != 1
//					? arr
//					: arr.get(0);

			String str = gson.toJson(tmp);
			System.out.println(str);
		}


		@Override
		public void flush() {
		}
	}