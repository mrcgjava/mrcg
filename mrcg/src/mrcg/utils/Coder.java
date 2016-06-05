package mrcg.utils;

/*
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

public class Coder {
	private StringBuilder b = new StringBuilder();
	
	public Coder print(int indent, String text) {
		doIndents(indent);
		b.append(text);
		return this;
	}
	
	public Coder println(int indent, String text) {
		doIndents(indent);
		b.append(text).append('\n');
		return this;
	}
	
	public String toString() {
		return b.toString();
	}
	
	private void doIndents(int indents) {
		for(int i = 0; i < indents; i++) b.append('\t');
	}	
}