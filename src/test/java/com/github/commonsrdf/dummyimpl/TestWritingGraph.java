/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.commonsrdf.dummyimpl;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.github.commonsrdf.api.BlankNode;
import com.github.commonsrdf.api.IRI;

public class TestWritingGraph {

	/** Run tests with -Dkeepfiles=true to inspect /tmp files **/
	private static boolean KEEP_FILES = Boolean.getBoolean("keepfiles");

	private GraphImpl graph;

	@Before
	public void createGraph() throws Exception {
		graph = new GraphImpl();
		BlankNode subject = new BlankNodeImpl("subj");
		IRI predicate = new IRIImpl("pred");
		// 200k triples should do
		for (int i = 0; i < 200000; i++) {
			graph.add(subject, predicate, new LiteralImpl("Example " + i, "en"));
		}
	}

	@Test
	public void writeGraphFromStream() throws Exception {
		Path graphFile = Files.createTempFile("graph", ".nt");
		if (KEEP_FILES) {
			System.out.println("From stream: " + graphFile);
		} else {
			graphFile.toFile().deleteOnExit();
		}

		Stream<CharSequence> stream = graph.getTriples().unordered().parallel()
				.map(Object::toString);
		Files.write(graphFile, stream::iterator, Charset.forName("UTF-8"));
	}

	@Test
	public void writeGraphFromStreamFiltered() throws Exception {
		Path graphFile = Files.createTempFile("graph", ".nt");
		if (KEEP_FILES) {
			System.out.println("Filtered stream: " + graphFile);
		} else {
			graphFile.toFile().deleteOnExit();
		}
		
		BlankNode subject = new BlankNodeImpl("subj");
		IRI predicate = new IRIImpl("pred");
		Stream<CharSequence> stream = graph
				.getTriples(subject, predicate, null).map(Object::toString);
		Files.write(graphFile, stream::iterator, Charset.forName("UTF-8"));

	}

}
