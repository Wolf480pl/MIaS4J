/*
 * Copyright (c) 2014 Wolf480pl <wolf480@interia.pl>
 * This program is licensed under the GNU Lesser General Public License.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class Test1 {

    public Test1(int i) {
        this(say("hey", i + 1), new LinkedList<Integer>());
        say(new Date(), 0);
    }

    public Test1(Integer i, List<Integer> l) {
        l.add(i);
    }

    public static void main(String[] args) {
        new Test1(10);
        System.out.println("byebye");

    }

    private static int say(Object o, int ret) {
        System.out.println(o);
        return ret;
    }

}
