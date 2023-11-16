Role Conjunction
================

.. contents:: :local:

Liferay permissions are additive. Essentially, once you have any role that grants you a permission, Liferay will assume you have that permission.

From time to time, we get asked the question, "Well, what if I want you to have *multiple* roles in order to have permission to perform an action? What would that code look like?"

Role Combinatorics
------------------

In order for something like that to work, you need to specify exactly how you want users to be able to specify that combinations of roles without having to enumerate all possibilities.

For example, let's say you decide that there are 18 regular roles spread across 3 role subtypes:

* **Location** (7): United States, Brazil, Spain, Hungary, China, Japan, India
* **Department** (6): Subscription Services, Customer Operations, Global Services, Customer Success, Marketing, Sales, Engineering
* **Position** (5): Chief Officer, Vice President, Director, Full Time Employee, Part Time Employee

You don't want to have to enumerate some subset of the 18 factorial different role combinations, but rather, you'd like some way to be able to both reduce the number of combinations involved as well as make that easy to understand.

One way to approach that is to apply the following restrictions:

* within each subtype (Location, Department, Position), we handle it as a disjunction
* across the subtypes, we handle it as a conjunction

This simplifying assumption allows us to make the following statement about how our permission system will work:

* some assets use regular Liferay permission checks using roles that are not of the given subtypes
* some assets require just Location, just Department, or just Position
* some assets require a combination of two (Location+Department, Location+Position, Department+Position)
* some assets require a combination of all three

Scope Interactions
------------------

Once you've defined that, you need to next make a decision on how scopes interact.

Usually in Liferay, when you specify that a role has a permission at a higher scope, because all permissions are additive, that role will have that permission on all applicable assets; the checkbox will be forcefully checked and disabled in the permissions UI, and all permission checks behave as such.

However, what about in a system where you are trying to require *multiple* roles? There are at least two ways to think about what the higher scope permission means:

* handle it the same way Liferay has always handled it
* handle it as if all assets of that type have the corresponding permission "checked" in the grid

Special Roles
-------------

Liferay usually grants special permissions to the "owner" of an asset. Should those owner permissions continue to apply, even after you've specified that certain roles are required (as in, can the owner lose access to an asset, and should we allow them to accidentally revoke access for themselves as they set permissions on that asset)?

What about administrators, site administrators, and other administrator-type roles where the UI doesn't give you the ability to revoke permissions?

User Interface
--------------

Also missing from this PoC is a user interface that allows people to easily see the combinations that have been set, as well as a way to add new entries to each role subtype.